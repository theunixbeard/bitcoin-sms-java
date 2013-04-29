/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.thirderapayments.spark_bitcoin_netbeans;

import com.google.bitcoin.core.BlockChain;
import com.google.bitcoin.core.PeerGroup;
import com.google.bitcoin.core.Wallet;
import com.google.bitcoin.store.BlockStore;
import java.io.File;

/**
 *
 * @author primary
 */
public class BitcoinData {
    private PeerGroup peerGroup;
    private BlockChain chain;
    private BlockStore blockStore;
    private File walletFile;
    private Wallet wallet;
    private int userId;
    private boolean completed;
    
  public BitcoinData(){
    peerGroup = null;
    chain = null;
    blockStore = null;
    walletFile = null;
    wallet = null;
    userId = -1;
    completed = false;
  }
  /**
   * @return the peerGroup
   */
  public PeerGroup getPeerGroup() {
    return peerGroup;
  }

  /**
   * @param peerGroup the peerGroup to set
   */
  public void setPeerGroup(PeerGroup peerGroup) {
    this.peerGroup = peerGroup;
  }

  /**
   * @return the chain
   */
  public BlockChain getChain() {
    return chain;
  }

  /**
   * @param chain the chain to set
   */
  public void setChain(BlockChain chain) {
    this.chain = chain;
  }

  /**
   * @return the blockStore
   */
  public BlockStore getBlockStore() {
    return blockStore;
  }

  /**
   * @param blockStore the blockStore to set
   */
  public void setBlockStore(BlockStore blockStore) {
    this.blockStore = blockStore;
  }

  /**
   * @return the walletFile
   */
  public File getWalletFile() {
    return walletFile;
  }

  /**
   * @param walletFile the walletFile to set
   */
  public void setWalletFile(File walletFile) {
    this.walletFile = walletFile;
  }

  /**
   * @return the userId
   */
  public int getUserId() {
    return userId;
  }

  /**
   * @param userId the userId to set
   */
  public void setUserId(int userId) {
    this.userId = userId;
  }

  /**
   * @return the completed
   */
  public boolean isCompleted() {
    return completed;
  }

  /**
   * @param completed the completed to set
   */
  public void setCompleted(boolean completed) {
    this.completed = completed;
  }

  /**
   * @return the wallet
   */
  public Wallet getWallet() {
    return wallet;
  }

  /**
   * @param wallet the wallet to set
   */
  public void setWallet(Wallet wallet) {
    this.wallet = wallet;
  }
    
    
    
}
