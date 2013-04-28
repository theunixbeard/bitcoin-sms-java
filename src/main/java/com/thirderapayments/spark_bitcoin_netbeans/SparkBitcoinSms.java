/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.thirderapayments.spark_bitcoin_netbeans;

/**
 *
 * @author Ben Gelsey
 */
import static spark.Spark.*;
import spark.*;

import com.google.bitcoin.utils.*;

import com.twilio.sdk.*;
import com.twilio.sdk.resource.factory.SmsFactory;
import com.twilio.sdk.resource.instance.Account;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SparkBitcoinSms {
  public static final String AccountSid = "ACb913e15900cc6f62af4da8ee49a2f460";
  public static final String AuthToken = "104c109df3dac74ce1860a6ffc30f86a";
  public static final String BenNumber = "+16092183692";
  public static final String ChristianNumber = "+12069099930";
  public static final String MaxNumber = "+16092401601";
  public static final String TwilioNumber = "+12673904385";
    public SparkBitcoinSms(String[] args) throws Exception {
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
    }
  
  /**
   * @param args the command line arguments
   */
  public static void main(String[] args) throws Exception  {
        BriefLogFormatter.init();
        new SparkBitcoinSms(args);

  }
}
