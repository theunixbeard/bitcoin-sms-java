/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.thirderapayments.spark_bitcoin_netbeans;

import com.google.bitcoin.core.*;
import com.google.bitcoin.crypto.*;
import com.google.bitcoin.discovery.*;
import com.google.bitcoin.store.*;
import com.google.bitcoin.utils.*;
import com.google.common.util.concurrent.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;

import static com.google.common.base.Preconditions.checkNotNull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author primary
 */
public class PingService {
  private List<BitcoinData> bitcoinData = new ArrayList<BitcoinData>();
  private List<String> addresses = new ArrayList<String>();
  private Map<Wallet, Integer> walletIndexMap = new HashMap<Wallet, Integer>(); 

    public static void main(String[] args) throws Exception {
        BriefLogFormatter.init();
        new PingService(args);
    }

    public PingService(String[] args) throws Exception {
        boolean testNet = args.length > 0 && args[0].equalsIgnoreCase("testnet");
        final NetworkParameters params = testNet ? NetworkParameters.testNet() : NetworkParameters.prodNet();
        String filePrefix = testNet ? "pingservice-testnet" : "pingservice-prodnet";
        // Added for multiple wallets 
        boolean multipleWallets = args.length > 1;
        int walletCount = 1;
        try{
          if(multipleWallets) {
             walletCount = Integer.parseInt(args[1]);
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
        //end Added for multiple wallets
        // Try to read the wallet from storage, create a new one if not possible.
        for(int i = 0; i < walletCount; ++i) {
          BitcoinData bitcoinDataItem = new BitcoinData();
          bitcoinData.add(bitcoinDataItem);
          File walletFile = new File(filePrefix + Integer.toString(i) + ".wallet");
          bitcoinDataItem.setWalletFile(walletFile);
          Wallet w;
          try {
              w = Wallet.loadFromFile(walletFile);
          } catch (IOException e) {
              w = new Wallet(params);
              w.keychain.add(new ECKey());
              w.saveToFile(walletFile);
          }
          final Wallet wallet = w;
          walletIndexMap.put(w, i);
          // Fetch the first key in the wallet (should be the only key).
          ECKey key = wallet.getKeys().iterator().next();
          // Load the block chain, if there is one stored locally. If it's going to be freshly created, checkpoint it.
          System.out.println("Reading block store from disk");
          File file = new File(filePrefix +  Integer.toString(i) + ".spvchain");
          boolean chainExistedAlready = file.exists();
          BlockStore blockStore = new SPVBlockStore(params, file);
          bitcoinDataItem.setBlockStore(blockStore);
          if (!chainExistedAlready) {
              File checkpointsFile = new File("checkpoints" + Integer.toString(i));
              if (checkpointsFile.exists()) {
                  FileInputStream stream = new FileInputStream(checkpointsFile);
                  CheckpointManager.checkpoint(params, stream, blockStore, key.getCreationTimeSeconds());
              }
          }
          BlockChain chain = new BlockChain(params, wallet, blockStore);
          bitcoinDataItem.setChain(chain);
          // Connect to the localhost node. One minute timeout since we won't try any other peers
          System.out.println("Connecting ...");
          PeerGroup peerGroup = new PeerGroup(params, chain);
          bitcoinDataItem.setPeerGroup(peerGroup);
          peerGroup.setUserAgent("PingService", "1.0");
          peerGroup.addPeerDiscovery(new DnsDiscovery(params));
          peerGroup.addWallet(wallet);

          // We want to know when the balance changes.
          wallet.addEventListener(new AbstractWalletEventListener() {
              @Override
              public void onCoinsReceived(Wallet w, Transaction tx, BigInteger prevBalance, BigInteger newBalance) {
                  // MUST BE THREAD SAFE
                  assert !newBalance.equals(BigInteger.ZERO);
                  if (!tx.isPending()) return;
                  // It was broadcast, but we can't really verify it's valid until it appears in a block.
                  BigInteger value = tx.getValueSentToMe(w);
                  System.out.println("Received pending tx for " + Utils.bitcoinValueToFriendlyString(value) +
                          ": " + tx);
                  tx.getConfidence().addEventListener(new TransactionConfidence.Listener() {
                      public void onConfidenceChanged(final Transaction tx2) {
                          // Must be thread safe.
                          if (tx2.getConfidence().getConfidenceType() == TransactionConfidence.ConfidenceType.BUILDING) {
                              // Coins were confirmed (appeared in a block).
                              tx2.getConfidence().removeEventListener(this);

                              // Run the process of sending the coins back on a separate thread. This is a temp hack
                              // until the threading changes in 0.9 are completed ... TX confidence listeners run
                              // with the wallet lock held and re-entering the wallet isn't always safe. We can solve
                              // this by just interacting with the wallet from a separate thread, which will wait until
                              // this thread is finished. It's a dumb API requirement and will go away soon.
                              new Thread() {
                                  @Override
                                  public void run() {
                                      bounceCoins(wallet, tx2);
                                  }
                              }.start();
                          } else {
                              System.out.println(String.format("Confidence of %s changed, is now: %s",
                                      tx2.getHashAsString(), tx2.getConfidence().toString()));
                          }
                      }
                  });
              }
          });

          peerGroup.startAndWait();
          // Now make sure that we shut down cleanly!
          Runtime.getRuntime().addShutdownHook(new Thread() {
              @Override public void run() {
                  try {
                      System.out.print("Shutting down ... ");
                      for(BitcoinData bitcoinDataItem : bitcoinData) {
                        bitcoinDataItem.getPeerGroup().stopAndWait();
                        wallet.saveToFile(bitcoinDataItem.getWalletFile());
                        bitcoinDataItem.getBlockStore().close();
                      }
                      System.out.print("done ");
                  } catch (Exception e) {
                      throw new RuntimeException(e);
                  }
              }
          });
          peerGroup.downloadBlockChain();
          addresses.add(key.toAddress(params).toString());

      }
      for(String address : addresses) {
          System.out.println("Send coins to: " + address);
      }
      System.out.println("Waiting for coins to arrive. Press Ctrl-C to quit.");
      try {
          Thread.sleep(Long.MAX_VALUE);
      } catch (InterruptedException e) {}
    }

    private void bounceCoins(final Wallet wallet, Transaction tx) {
        // It's impossible to pick one specific identity that you receive coins from in Bitcoin as there
        // could be inputs from many addresses. So instead we just pick the first and assume they were all
        // owned by the same person.
        try {
            // SLOW, ADD A MAP FOR QUICK LOOKUPS!!!
            int bitcoinDataIndex = walletIndexMap.get(wallet);
            
            BigInteger value = tx.getValueSentToMe(wallet);
            TransactionInput input = tx.getInputs().get(0);
            Address from = input.getFromAddress();
            System.out.println("Received " + Utils.bitcoinValueToFriendlyString(value) + " from " + from.toString());
            // Now send the coins back!
            final Wallet.SendResult sendResult = wallet.sendCoins(bitcoinData.get(bitcoinDataIndex).getPeerGroup(), from, value);
            checkNotNull(sendResult);  // We should never try to send more coins than we have!
            System.out.println("Sending ...");
            Futures.addCallback(sendResult.broadcastComplete, new FutureCallback<Transaction>() {
                public void onSuccess(Transaction transaction) {
                    System.out.println("Sent coins back! Transaction hash is " + sendResult.tx.getHashAsString());
                    // The wallet has changed now, it'll get auto saved shortly or when the app shuts down.
                }

                public void onFailure(Throwable throwable) {
                    System.err.println("Failed to send coins :(");
                    throwable.printStackTrace();
                }
            });
        } catch (ScriptException e) {
            // If we didn't understand the scriptSig, just crash.
            throw new RuntimeException(e);
        } catch (KeyCrypterException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}

