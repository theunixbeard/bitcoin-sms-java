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

public class HelloWorld {

   public static void main(String[] args) {

      setPort(6789);
      get(new Route("/") {
         @Override
         public Object handle(Request request, Response response) {
            return "Hello World!";
         }
      });

   }

}
