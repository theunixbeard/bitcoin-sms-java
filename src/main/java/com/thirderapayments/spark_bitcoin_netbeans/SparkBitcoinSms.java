/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.thirderapayments.spark_bitcoin_netbeans;

/**
 *
 * @author Ben Gelsey
 */
import com.google.bitcoin.core.AbstractWalletEventListener;
import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.BlockChain;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.PeerGroup;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionConfidence;
import com.google.bitcoin.core.TransactionOutput;
import com.google.bitcoin.core.Utils;
import com.google.bitcoin.core.Wallet;
import com.google.bitcoin.discovery.DnsDiscovery;
import com.google.bitcoin.store.BlockStore;
import com.google.bitcoin.store.SPVBlockStore;
import static spark.Spark.*;
import spark.*;

import com.google.bitcoin.utils.*;
import static com.google.common.base.Preconditions.checkNotNull;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;


import com.twilio.sdk.*;
import com.twilio.sdk.resource.factory.SmsFactory;
import com.twilio.sdk.resource.instance.Account;
import com.twilio.sdk.verbs.Sms;
import com.twilio.sdk.verbs.TwiMLException;
import com.twilio.sdk.verbs.TwiMLResponse;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;


public class SparkBitcoinSms {
  public static final String AccountSid = "ACb913e15900cc6f62af4da8ee49a2f460";
  public static final String AuthToken = "104c109df3dac74ce1860a6ffc30f86a";
  public static final String BenNumber = "+16092183692";
  public static final String ChristianNumber = "+12069099930";
  public static final String MaxNumber = "+16092401601";
  public static final String TwilioNumber = "+12673904385";
  public static final int TEXT_LENGTH = 160;
  public static final String TEXT_DELIMITER_STRING = "~~";
  
  
  private Random rand = new Random();
  private SecureRandom sec_rand = new SecureRandom();
  
  // Database 
  private Connection connect = null;
  private Statement statement = null;
  private PreparedStatement preparedStatement = null;
  private ResultSet resultSet = null;
  
  // Bitcoin
  private NetworkParameters params;
  private PeerGroup userPeerGroup;
  private BlockChain userChain;
  private BlockStore userBlockStore;
  private File userWalletFile;
  private PeerGroup companyPeerGroup;
  private BlockChain companyChain;
  private BlockStore companyBlockStore;
  private File companyWalletFile;
  private final Wallet userWallet;
  private final Wallet companyWallet;
  
  public SparkBitcoinSms(String[] args) throws Exception {
    // bitcoin init 1
    BitcoinData bitcoinDataContainer;
    bitcoinDataContainer = bitcoinInitialize(args, userPeerGroup, userChain, userBlockStore, userWalletFile, "User");
    // Necessary since Java is STRICTLY PASS BY VALUE!!!
    userPeerGroup = bitcoinDataContainer.getPeerGroup();
    userChain = bitcoinDataContainer.getChain();
    userBlockStore = bitcoinDataContainer.getBlockStore();
    userWalletFile = bitcoinDataContainer.getWalletFile();
    userWallet = bitcoinDataContainer.getWallet();
    bitcoinDataContainer = bitcoinInitialize(args, companyPeerGroup, companyChain, companyBlockStore, companyWalletFile, "Company");  
    companyPeerGroup = bitcoinDataContainer.getPeerGroup();
    companyChain = bitcoinDataContainer.getChain();
    companyBlockStore = bitcoinDataContainer.getBlockStore();
    companyWalletFile = bitcoinDataContainer.getWalletFile();
    companyWallet = bitcoinDataContainer.getWallet();
    try{
      // This will load the MySQL driver, each DB has its own driver
      Class.forName("com.mysql.jdbc.Driver");
      // Setup the connection with the DB
      connect = DriverManager.getConnection("jdbc:mysql://localhost:3306/SparkBitcoinSms?zeroDateTimeBehavior=convertToNull&"
               + "user=root");
    // bitcoin init 2
    bitcoinSetupListeners();
    bitcoinFinishInitialization();
      
      setPort(6789);
      get(new Route("/") {
         @Override
         public Object handle(Request request, Response response) {
            return "Hello World!";
         }
      });
      get(new Route("/send") {
        @Override
        public Object handle(Request request, Response response) {

          final TwilioRestClient client = new TwilioRestClient(AccountSid, AuthToken);
          // Get the main account (The one we used to authenticate the client)
          final Account mainAccount = client.getAccount();
          // Send an sms
          final SmsFactory smsFactory = mainAccount.getSmsFactory();
          final Map<String, String> smsParams = new HashMap<String, String>();
          smsParams.put("To", BenNumber); // Replace with a valid phone number
          smsParams.put("From", TwilioNumber); // Replace with a valid phone number in your account
          smsParams.put("Body", "java twilio wut");
          try {
            smsFactory.create(smsParams);
          } catch (TwilioRestException ex) {
            Logger.getLogger(SparkBitcoinSms.class.getName()).log(Level.SEVERE, null, ex);
          }
          return "Message Sent";
        }
      });
      get(new Route("/balance") {
        @Override
        public Object handle(Request request, Response response) {
          String ret = "User Wallet Value: " + userWallet.getBalance(Wallet.BalanceType.ESTIMATED).toString();
          String ret2 = "Company Wallet Value: " + companyWallet.getBalance(Wallet.BalanceType.ESTIMATED).toString();
          System.out.println(ret);
          System.out.println(ret2);
          return ret + "\n" + ret2;
        }
      });
      get(new Route("/test") {
        @Override
        public Object handle(Request request, Response response) {
          try {
          // PreparedStatements can use variables and are more efficient
          preparedStatement = connect
            .prepareStatement("insert into  SparkBitcoinSms.UnconfirmedUsers "
                  + "(id, name, phoneNumber, balance, recoveryPassword, confirmationCode, created, updated) "
                  + "values "
                  + "(default, ?, ?, ?, ? , ?, default, default)");
          // id, name, phoneNumber, balance, 
          // recoveryPassword, confirmationCode, created, updated
          // Parameters start with 1
          preparedStatement.setString(1, "John Doe" + Integer.toString(rand.nextInt()));
          preparedStatement.setString(2, "1112223333");
          preparedStatement.setInt(3, 500);
          preparedStatement.setString(4, "RecoverMePlz");
          preparedStatement.setString(5, "1234567890");
          preparedStatement.executeUpdate();
          // Statements allow to issue SQL queries to the database
          statement = connect.createStatement();
          // Result set get the result of the SQL query
          resultSet = statement.executeQuery("select * from SparkBitcoinSms.UnconfirmedUsers");
          while (resultSet.next()) {
            System.out.println("Name: " + resultSet.getString("name"));
          }
          } catch (Exception e) {
            e.printStackTrace();
          }
          return "this was a test";
        }
      });
      get(new Route("/receive") {
        @Override
        public Object handle(Request request, Response response) {
          /*
          for(String param : request.queryParams()) {
            System.out.println("param: " + param + " val: "+ request.queryParams(param));
          }*/
          String responseText = "";
          Session session = request.session(true); // true means create if it doesn't exist
          boolean isNewSession = true;
          String actionPath = session.attribute("actionPath");
          if(actionPath != null){
            isNewSession = false;
          }
          String smsBody = request.queryParams("Body");
          String smsFrom = request.queryParams("From");
          System.out.println("body query param: " + smsBody);
          if(smsBody == null){
            return "error: no body query param";
          }
          if(isNewSession) {
            responseText = parseNewConversationSmsBody(smsBody, session, request);
          }else {
            if(actionPath.equals("signup")){
              responseText = signup2(smsBody, session);
              System.out.println("signup2 response text: " + responseText);
            } else if(actionPath.equals("send")) {
              responseText = userSendRequest2(smsBody, request, session);
              System.out.println("sendrequest2 response text: " + responseText);
            } else{
              responseText = "error: unknown session";
            }
          }
          TwiMLResponse twiml = new TwiMLResponse();
          composeTextResponseFromString(twiml, responseText);
          response.type("application/xml"); 
          System.out.println("Done with response");
          return twiml.toXML();
        }
      });
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      Runtime.getRuntime().addShutdownHook(new Thread() {
        @Override public void run() {
          try {
            System.out.print("Shutting down ... ");
            connect.close();
            System.out.print("database connection closed");
            System.out.print("done ");
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        }
        });
    }
  }
  
  private String parseNewConversationSmsBody(String smsBody, Session session, Request request) {
    String lines[] = smsBody.split("\n");
    String response = "";
    for(String line : lines) {
      System.out.println(line);
    }
    if(lines.length <= 0) {
      return "error: empty message ";
    }
    if(lines[0].equals("signup")){
      response = signup(lines, session);
    }else if(lines[0].equals("a")){
      System.out.println("processing userAddressRequest");
      response = userAddressRequest(smsBody, request);
    } else if(lines[0].equals("cb")) {
      System.out.println("processing userCheckBalanceRequest");
      response = userCheckBalanceRequest(smsBody, request);
    } else if(lines[0].equals("send")) {
      System.out.println("processing userSendRequest1");
      response = userSendRequest1(smsBody, request, session);
    } else if(lines[0].equals("withdraw")) {
      System.out.println("processing userWithdrawRequest1");
      response = userWithdrawRequest1(smsBody, request, session);
    } else if(lines[0].equals("withdraw confirm")) {
      System.out.println("processing agentWithdrawConfirm");
      response = agentWithdrawConfirm(smsBody, request, session);
    } else {
      System.out.println("unknown message type: " + smsBody);
      response = "unknown message type"; //Add in what known message types are too!!!
    }
    return response;
  }
  
  private String signup(String lines[], Session session){
    String response = "";
    if(!signupValidations(lines)){
      response = "error: bad signup message";
      return response;
    }
    try {
      String name = lines[1];
      String phoneNumber = lines[2];
      //Twilio always prefixes with a '+', so will we
      phoneNumber = "+" + phoneNumber;
      long amount = Long.parseLong(lines[3]);
      String recoveryPassword = lines[4];
      String confirmationCode = new BigInteger(20, sec_rand).toString(32);
      System.out.println("Confirmation code: " + confirmationCode);
      preparedStatement = connect
        .prepareStatement("insert into  SparkBitcoinSms.UnconfirmedUsers "
              + "(id, name, phoneNumber, balance, recoveryPassword, confirmationCode, created, updated) "
              + "values "
              + "(default, ?, ?, ?, ? , ?, default, default)");
      // id, name, phoneNumber, balance, 
      // recoveryPassword, confirmationCode, created, updated
      // Parameters start with 1
      preparedStatement.setString(1, name);
      preparedStatement.setString(2, phoneNumber);
      preparedStatement.setLong(3, amount);
      preparedStatement.setString(4, recoveryPassword);
      preparedStatement.setString(5, confirmationCode);
      preparedStatement.executeUpdate();
      response = "Please reply with " + confirmationCode + " to complete account signup";
      session.attribute("actionPath", "signup");
      session.attribute("signupPhoneNumber", phoneNumber);
    } catch(Exception e){
      response = "error: something went wrong on our end!";
      e.printStackTrace();
    }
    return response;
  }
  
  private boolean signupValidations(String lines[]){
    /*
     * Message format is:
     * signup
     * name
     * phone number
     * amount
     * recovery passphrase
     */
            
    if(!lines[0].equals("signup")){
      System.out.println("signup validation fail: first line not signup");
      return false;
    }
    if(lines.length != 5){
      System.out.println("signup validation fail:  not 5 lines in message");
      return false;
    }
    String phoneNumber = lines[2];
    if(!phoneNumber.matches("^\\d{11}$")) {
      System.out.println("signup validation fail: not 11 digits");
      return false;
    }
    long amount = -1;
    try {
      amount = Long.parseLong(lines[3]);
    } catch(NumberFormatException e){
      System.out.println("signup validation fail: not valid amount");
      return false;      
    }
    if(amount < 0) {
      System.out.println("signup validation fail: negative amount");
      return false;
    }
    return true;          
  }
  
  private String signup2(String smsBody, Session session){
    String unconfirmedUserPhoneNumber = session.attribute("signupPhoneNumber");
    if(unconfirmedUserPhoneNumber == null) {
      return "error: no user phone number in session";
    }
    session.removeAttribute("actionPath");
    session.removeAttribute("phoneNumber");    
    System.out.println("User was in signup action path, submitted: " + smsBody);
    try {
      preparedStatement = connect.prepareStatement("select * from SparkBitcoinSms.UnconfirmedUsers where phoneNumber=?");
      preparedStatement.setString(1, unconfirmedUserPhoneNumber);
      resultSet = preparedStatement.executeQuery();
      System.out.println(preparedStatement.toString());
      boolean signupOccurred = false;
      while (resultSet.next()) {
        if(resultSet.getString("confirmationCode").equals(smsBody)) {
          if(signupOccurred) {
            System.err.println("error: two or more unconfirmed users have been confirmed by one code");
          }
          signupOccurred = true;
          System.out.println("signup confirmed for " + resultSet.getString("name") + 
                  " with code: " + resultSet.getString("confirmationCode"));
          // Create bitcoin address for user
          ECKey newUserBitcoinECKey = new ECKey();
          userWallet.addKey(newUserBitcoinECKey);
          userWallet.saveToFile(userWalletFile);
          // Send bitcoins from company wallet to user wallet
          final Wallet.SendResult sendResult = companyWallet.sendCoins(
                  companyPeerGroup, 
                  newUserBitcoinECKey.toAddress(params), 
                  BigInteger.valueOf(resultSet.getLong("balance")));
          checkNotNull(sendResult);  // We should never try to send more coins than we have!
          System.out.println("Sending ...");
          Futures.addCallback(sendResult.broadcastComplete, new FutureCallback<Transaction>() {
              public void onSuccess(Transaction transaction) {
                  System.out.println("Sent coins! Transaction hash is " + sendResult.tx.getHashAsString());
                  // The wallet has changed now, it'll get auto saved shortly or when the app shuts down.
                  try { companyWallet.saveToFile(companyWalletFile);} catch(IOException e){}
              }

              public void onFailure(Throwable throwable) {
                  System.err.println("Failed to send coins :(");
                  throwable.printStackTrace();
              }
          });
          preparedStatement = connect
            .prepareStatement("insert into  SparkBitcoinSms.Users "
                  + "(id, name, phoneNumber, balance, recoveryPassword, bitcoinAddress, created, updated) "
                  + "values "
                  + "(default, ?, ?, ?, ?, ?, default, default)");
          // id, name, phoneNumber, balance, 
          // recoveryPassword, created, updated
          // Parameters start with 1
          preparedStatement.setString(1, resultSet.getString("name"));
          preparedStatement.setString(2, resultSet.getString("phoneNumber"));
          preparedStatement.setLong(3, resultSet.getLong("balance"));
          preparedStatement.setString(4, resultSet.getString("recoveryPassword"));
          preparedStatement.setString(5, newUserBitcoinECKey.toAddress(params).toString());
          preparedStatement.executeUpdate();
          System.out.println("Added " + resultSet.getString("name") + " to Users table");
          // Now remove from UnconfirmedUsers
          String sql = "delete from UnconfirmedUsers where id=?";
          preparedStatement = connect.prepareStatement(sql);
          preparedStatement.setLong(1, resultSet.getLong("id"));
          int rowsDeleted = preparedStatement.executeUpdate();
          System.out.println(rowsDeleted + " rows deleted from unconfirmed users table.");
        }
      }
    } catch(Exception e) {
      e.printStackTrace();
      return "error: something went wrong on our end";
    }
      return "Signup successful. Commands are:\n"
              + "Send 'cb' for check balance.\n"
              + "Send 'a' for your bitcoin address.\n\n(1/2)~~"
              + "To send money to another user, send a message of the format:\n"
              + "send\n"
              + "[phone number]\n"
              + "[amount]\n\n(2/2)";  
  }
  
  private void composeTextResponseFromString(TwiMLResponse twiml, String responseText) {
    List<Sms> smses = new ArrayList<Sms>();
    if(responseText.length() < TEXT_LENGTH) {
      smses.add(new Sms(responseText));
    }else {
      String responses[] = responseText.split(TEXT_DELIMITER_STRING);
      for(String response : responses) {
        if(response.length() < TEXT_LENGTH) {
          smses.add(new Sms(response));
        }else {
          List<String> smsStrings = splitStringIntoTextChunks(response);
          for(String smsString : smsStrings) {
            smses.add(new Sms(smsString));;
          }
        }
      }
    }
    // Seems to help with message ordering
    List<Sms> reversed = Lists.reverse(smses);
    try {
      for(Sms sms : reversed) {
        twiml.append(sms);
      }
    } catch (TwiMLException e) {
      e.printStackTrace();
    }
    System.out.println("Done creating smses");
    return;
  }
  
  List<String> splitStringIntoTextChunks(String s){
    List<String> chunks = new ArrayList<String>();
    int start = 0;
    int end = TEXT_LENGTH;;
    while(end < s.length()){
      chunks.add(s.substring(start, end));
      start = end;
      end += 160;
    }
    chunks.add(s.substring(start, s.length()));
    return chunks;
  }
  
  String userAddressRequest(String smsBody, Request request){
    String response = "";
    String smsFrom = request.queryParams("From");
    try {
      preparedStatement = connect.prepareStatement("select bitcoinAddress from SparkBitcoinSms.Users where phoneNumber=?");
      preparedStatement.setString(1, smsFrom);
      resultSet = preparedStatement.executeQuery();
      System.out.println(preparedStatement.toString());
      boolean userPhoneNumberMatched = false;
      while (resultSet.next()) {
        if(!userPhoneNumberMatched) {
          response = "Your bitcoin address is: \n " + resultSet.getString("bitcoinAddress");
        }else {
          System.out.println("possible error: multiple users with phone number " + smsFrom);
        }
        userPhoneNumberMatched = true;
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    System.out.println("userAddressRequest response: " + response);
    return response;
  }
  
  String userCheckBalanceRequest(String smsBody, Request request){
    String response = "";
    String smsFrom = request.queryParams("From");
    long balance = getBalanceForPhoneNumber(smsFrom);
    response = "Your balance is: \n " + balance + " satoshis" +
        "\n (" + (balance / Math.pow(10, 5)) + " millibtc)";
    System.out.println("userAddressRequest response: " + response);
    return response;
  }
  
  String userSendRequest1(String smsBody, Request request, Session session){
    String response = "";
    String lines[] = smsBody.split("\n");
    if(!sendValidations(lines)){
      response = "error: bad send initialization message";
      return response;
    }
    try {
      String phoneNumberTo = lines[1];
      //Twilio always prefixes with a '+', so will we
      phoneNumberTo = "+" + phoneNumberTo;
      String phoneNumberFrom = request.queryParams("From");
      long amount = Long.parseLong(lines[2]);
      // Make sure they have enough in their account to send!!!!!!
      long balance = getBalanceForPhoneNumber(phoneNumberFrom);
      if(amount > balance) {
        response = "error: not enough money in account";
        return response;
      }
      String confirmationCode = new BigInteger(20, sec_rand).toString(32);
      System.out.println("Confirmation code: " + confirmationCode);
      preparedStatement = connect
        .prepareStatement("insert into  SparkBitcoinSms.UnconfirmedTransfers "
              + "(id, phoneNumberTo, phoneNumberFrom, amount, confirmationCode, created, updated) "
              + "values "
              + "(default, ?, ?, ?, ?, default, default)");
      preparedStatement.setString(1, phoneNumberTo);
      preparedStatement.setString(2, phoneNumberFrom);
      preparedStatement.setLong(3, amount);
      preparedStatement.setString(4, confirmationCode);
      preparedStatement.executeUpdate();
      response = "Please reply with " + confirmationCode + " to complete transfer of: " +
              Long.toString(amount) + " to " + phoneNumberTo.substring(1);
      session.attribute("actionPath", "send");
    } catch(Exception e){
      response = "error: something went wrong on our end!";
      e.printStackTrace();
    }
    return response;
  }

  String userSendRequest2(String smsBody, Request request, Session session) {
    String unconfirmedUserPhoneNumber = request.queryParams("From");
    if(unconfirmedUserPhoneNumber == null) {
      return "error: no user phone number in session";
    }
    session.removeAttribute("actionPath");    
    System.out.println("User was in send action path, submitted: " + smsBody);
    try {
      preparedStatement = connect.prepareStatement("select * from SparkBitcoinSms.UnconfirmedTransfers where phoneNumberFrom=?");
      preparedStatement.setString(1, unconfirmedUserPhoneNumber);
      resultSet = preparedStatement.executeQuery();
      System.out.println(preparedStatement.toString());
      boolean transferOccurred = false;
      while (resultSet.next()) {
        if(resultSet.getString("confirmationCode").equals(smsBody)) {
          if(transferOccurred) {
            System.err.println("error: two or more unconfirmed transfers have been confirmed by one code");
          }
          transferOccurred = true;
          System.out.println("transfer confirmed for " + resultSet.getString("phoneNumberFrom") + 
                  " with code: " + resultSet.getString("confirmationCode"));
          // Debit the sender and credit the receiver in the database
          /*
           * Need:
           * Sender Number
           * Receiver Number
           * Sender Old Balance
           * Receiver Old Balance
           * Amount Sent
           */
          String fromNumber = resultSet.getString("phoneNumberFrom");
          String toNumber = resultSet.getString("phoneNumberTo");
          long amountSent = resultSet.getLong("amount");
          long oldSenderBalance = getBalanceForPhoneNumber(fromNumber);
          long oldReceiverBalance = getBalanceForPhoneNumber(toNumber);
          String sql = "UPDATE SparkBitcoinSms.Users SET balance = ? WHERE phoneNumber = ?";
          preparedStatement = connect.prepareStatement(sql);
          preparedStatement.setLong(1, oldSenderBalance - amountSent);
          preparedStatement.setString(2, fromNumber);
          preparedStatement.executeUpdate();
          preparedStatement.setLong(1, oldReceiverBalance + amountSent);
          preparedStatement.setString(2, toNumber);
          preparedStatement.executeUpdate();
          // Added to Transfers
          preparedStatement = connect
            .prepareStatement("insert into  SparkBitcoinSms.Transfers "
                  + "(id, phoneNumberTo, phoneNumberFrom, amount, created, updated) "
                  + "values "
                  + "(default, ?, ?, ?, default, default)");
          preparedStatement.setString(1, resultSet.getString("phoneNumberTo"));
          preparedStatement.setString(2, resultSet.getString("phoneNumberFrom"));
          preparedStatement.setLong(3, resultSet.getLong("amount"));
          preparedStatement.executeUpdate();
          System.out.println("Added entry to Transfers table");
          // Now remove from UnconfirmedUsers
          sql = "delete from UnconfirmedTransfers where id=?";
          preparedStatement = connect.prepareStatement(sql);
          preparedStatement.setLong(1, resultSet.getLong("id"));
          int rowsDeleted = preparedStatement.executeUpdate();
          System.out.println(rowsDeleted + " rows deleted from unconfirmed transfers table.");
        }
      }
    } catch(Exception e) {
      e.printStackTrace();
      return "error: something went wrong on our end";
    }
      return "Send successful.";  
  }
  
  long getBalanceForPhoneNumber(String phoneNumber) {
    try {
      PreparedStatement preparedStatementLocal = connect.prepareStatement("select balance from SparkBitcoinSms.Users where phoneNumber=?");
      preparedStatementLocal.setString(1, phoneNumber);
      ResultSet resultSetLocal = preparedStatementLocal.executeQuery();
      System.out.println(preparedStatementLocal.toString());
      while (resultSetLocal.next()) {
        return resultSetLocal.getLong("balance");
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return -1;
  }
  
  boolean sendValidations(String lines[]) {
   /*
     * Message format is:
     * send
     * phone number to (or btc address) [btc address not implemented yet]
     * amount
     */
            
    if(!lines[0].equals("send")){
      System.out.println("send validation fail: first line not send");
      return false;
    }
    if(lines.length != 3){
      System.out.println("signup validation fail:  not 3 lines in message");
      return false;
    }
    String phoneNumber = lines[1];
    if(!phoneNumber.matches("^\\d{11}$")) {
      System.out.println("signup validation fail: not 11 digits");
      return false;
    }
    long amount = -1;
    try {
      amount = Long.parseLong(lines[2]);
    } catch(NumberFormatException e){
      System.out.println("signup validation fail: not valid amount");
      return false;      
    }
    if(amount < 0) {
      System.out.println("signup validation fail: negative amount");
      return false;
    }
    return true;          
  }
  
  boolean sendSms(String number, String message) {
    final TwilioRestClient client = new TwilioRestClient(AccountSid, AuthToken);
    // Get the main account (The one we used to authenticate the client)
    final Account mainAccount = client.getAccount();
    // Send an sms
    final SmsFactory smsFactory = mainAccount.getSmsFactory();
    final Map<String, String> smsParams = new HashMap<String, String>();
    smsParams.put("To", number); // Replace with a valid phone number
    smsParams.put("From", TwilioNumber); // Replace with a valid phone number in your account
    smsParams.put("Body", message);
    try {
      smsFactory.create(smsParams);
      return true;
    } catch (TwilioRestException ex) {
      Logger.getLogger(SparkBitcoinSms.class.getName()).log(Level.SEVERE, null, ex);
    }
    return false;
  }
  
  String userWithdrawRequest1(String smsBody, Request request, Session session) {
    String response = "";
    String lines[] = smsBody.split("\n");
    if(!withdrawValidations(lines)){
      response = "error: bad withdraw initialization message";
      return response;
    }
    try {
      String phoneNumberFrom = request.queryParams("From");
      long amount = Long.parseLong(lines[1]);
      // Make sure they have enough in their account to send!!!!!!
      long balance = getBalanceForPhoneNumber(phoneNumberFrom);
      if(amount > balance) {
        response = "error: not enough money in account for that withdrawal";
        return response;
      }
      String confirmationCode = new BigInteger(20, sec_rand).toString(32);
      System.out.println("Confirmation code: " + confirmationCode);
      preparedStatement = connect
        .prepareStatement("insert into  SparkBitcoinSms.UnconfirmedWithDrawals "
              + "(id, phoneNumber, amount, confirmationCode, created, updated) "
              + "values "
              + "(default, ?, ?, ?, default, default)");
      preparedStatement.setString(1, phoneNumberFrom);
      preparedStatement.setLong(2, amount);
      preparedStatement.setString(3, confirmationCode);
      preparedStatement.executeUpdate();
      response = "Please give a BitBeam agent the following code: " + confirmationCode + 
              " along with your phone number to complete withdrawal of " +
              Long.toString(amount);
    } catch(Exception e){
      response = "error: something went wrong on our end!";
      e.printStackTrace();
    }
    return response;
  }
  
    boolean withdrawValidations(String lines[]) {
   /*
     * Message format is:
     * withdraw
     * amount
     */
            
    if(!lines[0].equals("withdraw")){
      System.out.println("withdraw validation fail: first line not withdraw");
      return false;
    }
    if(lines.length != 2){
      System.out.println("withdraw validation fail:  not 2 lines in message");
      return false;
    }
    long amount = -1;
    try {
      amount = Long.parseLong(lines[1]);
    } catch(NumberFormatException e){
      System.out.println("withdraw validation fail: not valid amount");
      return false;      
    }
    if(amount < 0) {
      System.out.println("withdraw validation fail: negative amount");
      return false;
    }
    return true;          
  }
  
  String agentWithdrawConfirm(String smsBody, Request request, Session session) {
    String response = "";
    if(!agentWithdrawValidations(smsBody.split("\n"))){
      response = "error: bad signup message";
      return response;
    }
    String unconfirmedUserPhoneNumber = request.queryParams("From");
    if(unconfirmedUserPhoneNumber == null) {
      return "error: no user phone number in session";
    }
    session.removeAttribute("actionPath");    
    System.out.println("User was in send action path, submitted: " + smsBody);
    try {
      preparedStatement = connect.prepareStatement("select * from SparkBitcoinSms.UnconfirmedTransfers where phoneNumberFrom=?");
      preparedStatement.setString(1, unconfirmedUserPhoneNumber);
      resultSet = preparedStatement.executeQuery();
      System.out.println(preparedStatement.toString());
      boolean transferOccurred = false;
      while (resultSet.next()) {
        if(resultSet.getString("confirmationCode").equals(smsBody)) {
          if(transferOccurred) {
            System.err.println("error: two or more unconfirmed transfers have been confirmed by one code");
          }
          transferOccurred = true;
          System.out.println("transfer confirmed for " + resultSet.getString("phoneNumberFrom") + 
                  " with code: " + resultSet.getString("confirmationCode"));
          // Debit the sender and credit the receiver in the database
          /*
           * Need:
           * Sender Number
           * Receiver Number
           * Sender Old Balance
           * Receiver Old Balance
           * Amount Sent
           */
          String fromNumber = resultSet.getString("phoneNumberFrom");
          String toNumber = resultSet.getString("phoneNumberTo");
          long amountSent = resultSet.getLong("amount");
          long oldSenderBalance = getBalanceForPhoneNumber(fromNumber);
          long oldReceiverBalance = getBalanceForPhoneNumber(toNumber);
          String sql = "UPDATE SparkBitcoinSms.Users SET balance = ? WHERE phoneNumber = ?";
          preparedStatement = connect.prepareStatement(sql);
          preparedStatement.setLong(1, oldSenderBalance - amountSent);
          preparedStatement.setString(2, fromNumber);
          preparedStatement.executeUpdate();
          preparedStatement.setLong(1, oldReceiverBalance + amountSent);
          preparedStatement.setString(2, toNumber);
          preparedStatement.executeUpdate();
          // Added to Transfers
          preparedStatement = connect
            .prepareStatement("insert into  SparkBitcoinSms.Transfers "
                  + "(id, phoneNumberTo, phoneNumberFrom, amount, created, updated) "
                  + "values "
                  + "(default, ?, ?, ?, default, default)");
          preparedStatement.setString(1, resultSet.getString("phoneNumberTo"));
          preparedStatement.setString(2, resultSet.getString("phoneNumberFrom"));
          preparedStatement.setLong(3, resultSet.getLong("amount"));
          preparedStatement.executeUpdate();
          System.out.println("Added entry to Transfers table");
          // Now remove from UnconfirmedUsers
          sql = "delete from UnconfirmedTransfers where id=?";
          preparedStatement = connect.prepareStatement(sql);
          preparedStatement.setLong(1, resultSet.getLong("id"));
          int rowsDeleted = preparedStatement.executeUpdate();
          System.out.println(rowsDeleted + " rows deleted from unconfirmed transfers table.");
        }
      }
    } catch(Exception e) {
      e.printStackTrace();
      return "error: something went wrong on our end";
    }
      return "Send successful."; 
  }
  
   boolean agentWithdrawValidations(String lines[]) {
   /*
     * Message format is:
     * withdraw confirm
     * phonenumber
     */
            
    if(!lines[0].equals("withdraw confirm")){
      System.out.println("withdraw-confirm validation fail: first line not 'withdraw confirm'");
      return false;
    }
    if(lines.length != 2){
      System.out.println("withdraw-confirm validation fail: not 2 lines in message");
      return false;
    }
    String phoneNumber = lines[1];
    if(!phoneNumber.matches("^\\d{11}$")) {
      System.out.println("withdraw-confirm validation fail: not 11 digits");
      return false;
    }
    return true;          
  }
    
  BitcoinData bitcoinInitialize(String[] args, PeerGroup peerGroup, BlockChain chain, BlockStore blockStore, File walletFile, String filePrefixType) throws Exception{
        BitcoinData ret = new BitcoinData();
        boolean testNet = args.length > 0 && args[0].equalsIgnoreCase("testnet");
        params = testNet ? NetworkParameters.testNet() : NetworkParameters.prodNet();
        String filePrefix = testNet ? "SparkBitcoinSms-testnet" : "SparkBitcoinSms-prodnet";
        filePrefix = filePrefixType + filePrefix;
        // Try to read the wallet from storage, create a new one if not possible.
        walletFile = new File(filePrefix + ".wallet");
        Wallet wallet;
        try {
          wallet = Wallet.loadFromFile(walletFile);
        } catch (IOException e) {
          System.err.println("ERROR: COULD NOT FIND " + filePrefixType + " WALLET FILE, SHOULD NEVER OCCUR OTHER THAN FIRST RUN!");
          wallet = new Wallet(params);
          if(filePrefixType.equals("Company")) {
            wallet.keychain.add(new ECKey());
          }
          wallet.saveToFile(walletFile);
        }
        System.out.println("Reading block store from disk for" + filePrefixType + " wallet");
        File file = new File(filePrefix + ".spvchain");
        blockStore = new SPVBlockStore(params, file);
        chain = new BlockChain(params, wallet, blockStore);
        // Connect to the localhost node. One minute timeout since we won't try any other peers
        System.out.println(filePrefixType + " Connecting ...");
        peerGroup = new PeerGroup(params, chain);
        peerGroup.setUserAgent(filePrefixType + "SparkBitcoinSms", "1.0");
        peerGroup.addPeerDiscovery(new DnsDiscovery(params));
        peerGroup.addWallet(wallet);
        ret.setPeerGroup(peerGroup);
        ret.setBlockStore(blockStore);
        ret.setChain(chain);
        ret.setWalletFile(walletFile);
        ret.setWallet(wallet);
        return ret;
  }
  
  void bitcoinSetupListeners() {
       // User Wallet Listener
        userWallet.addEventListener(new AbstractWalletEventListener() {
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
                                  //TODO: ADD CUSTOM LOGIC METHOD FOR USER RECEIVING COINS
                                  try{
                                    userWallet.saveToFile(userWalletFile);
                                  } catch (IOException e) {
                                    e.printStackTrace();
                                    System.err.println("Error: Couldn't Save User Wallet...");
                                  }
                                  System.out.println("MY CUSTOM LOGIC HERE TO HANDLE COIN RECEIPT FOR USERS");
                                  // Find out how much the user received, and credit their account via the DB
                                  for(TransactionOutput txOut : tx2.getOutputs()) {
                                    BigInteger amountSent = txOut.getValue();
                                    String bitcoinAddressString = "";
                                    try {
                                      bitcoinAddressString = txOut.getScriptPubKey().getToAddress().toString();
                                      String sql = "select * from SparkBitcoinSms.Users where bitcoinAddress = ?";
                                      preparedStatement = connect.prepareStatement(sql);
                                      preparedStatement.setString(1, bitcoinAddressString);
                                      resultSet = preparedStatement.executeQuery();
                                      boolean userBitcoinAddressMatched = false;
                                      while (resultSet.next()) {
                                        if(!userBitcoinAddressMatched) {
                                          String number = resultSet.getString("phoneNumber");
                                          long oldBalance = resultSet.getLong("balance");
                                          long newBalance = oldBalance + amountSent.longValue();
                                          sql = "UPDATE SparkBitcoinSms.Users SET balance = ? WHERE bitcoinAddress = ?";
                                          preparedStatement = connect.prepareStatement(sql);
                                          preparedStatement.setLong(1, newBalance);
                                          preparedStatement.setString(2, bitcoinAddressString);
                                          preparedStatement.executeUpdate();
                                          System.out.println("updated balance of address " + bitcoinAddressString +
                                                  " from " + oldBalance + " to " + newBalance);
                                          sendSms(number, "You got paid :)\n Your new balance is: " + Long.toString(newBalance)
                                                  + "\n(" + (newBalance / Math.pow(10, 5)) + " millibtc)");
                                        }else {
                                          System.out.println("possible error: multiple users with bitcoin address " + bitcoinAddressString);
                                        }
                                        userBitcoinAddressMatched = true;
                                      }

                                    } catch (Exception e) {
                                      e.printStackTrace();
                                    }
                                  }
                                  //bounceCoins(wallet, tx2);
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
      // Company Wallet Listener
        companyWallet.addEventListener(new AbstractWalletEventListener() {
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
                                  try{
                                    companyWallet.saveToFile(companyWalletFile);
                                  } catch (IOException e) {
                                    e.printStackTrace();
                                    System.err.println("Error: Couldn't Save Company Wallet...");
                                  }
                                  //TODO: ADD CUSTOM LOGIC METHOD
                                    System.out.println("MY CUSTOM LOGIC HERE TO HANDLE COIN RECEIPT FOR COMPANY");
                                    //bounceCoins(wallet, tx2);
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
  }
  
  void bitcoinFinishInitialization() {
        userPeerGroup.startAndWait();
        companyPeerGroup.startAndWait();      
        // Now make sure that we shut down cleanly!
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override public void run() {
                try {
                    System.out.print("Shutting down ... ");
                    userPeerGroup.stopAndWait();
                    companyPeerGroup.stopAndWait();
                    userWallet.saveToFile(userWalletFile);                
                    companyWallet.saveToFile(companyWalletFile);
                    userBlockStore.close();
                    companyBlockStore.close();
                    System.out.print("done ");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
        userPeerGroup.downloadBlockChain();
        companyPeerGroup.downloadBlockChain();
        // Print addresses
        System.out.println("Company Wallet Address: " + companyWallet.getKeys().iterator().next().toAddress(params).toString());
        System.out.println("User Addresses: ");
        Iterator<ECKey> userKeyIterator = userWallet.getKeys().iterator();
        while(userKeyIterator.hasNext()) {
          ECKey userKey = userKeyIterator.next();
          System.out.println("User Wallet Address: " + userKey.toAddress(params).toString());
        }
  }
  
  /**
   * @param args the command line arguments
   */
  public static void main(String[] args) throws Exception  {
        BriefLogFormatter.init();
        new SparkBitcoinSms(args);

  }
}
