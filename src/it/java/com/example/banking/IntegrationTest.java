package com.example.banking;

import com.example.banking.transaction.TransactionController;
import com.example.banking.user.UserController;
import com.example.banking.account.AccountController;
import com.example.banking.transaction.DomainModel;
import com.google.protobuf.any.Any;
import kalix.javasdk.DeferredCall;
import kalix.spring.testkit.KalixIntegrationTestKitSupport;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.example.banking.account.AccountApiModel.CreateAccountRequest;
import static com.example.banking.transaction.TransactionApiModel.TransactionProcessRequest;
import static com.example.banking.user.UserApiModel.CreateUserRequest;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.awaitility.Awaitility.*;


/**
 * This is a skeleton for implementing integration tests for a Kalix application built with the Java SDK.
 *
 * This test will initiate a Kalix Proxy using testcontainers and therefore it's required to have Docker installed
 * on your machine. This test will also start your Spring Boot application.
 *
 * Since this is an integration tests, it interacts with the application using a WebClient
 * (already configured and provided automatically through injection).
 */
@SpringBootTest(classes = Main.class)
public class IntegrationTest extends KalixIntegrationTestKitSupport {

  private final Logger log = LoggerFactory.getLogger(getClass());

  private <T> T execute(DeferredCall<Any, T> deferredCall) {
    try {
      return deferredCall.execute().toCompletableFuture().get(20, TimeUnit.SECONDS);
    } catch (InterruptedException | ExecutionException | TimeoutException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void testHappyPath() throws Exception {
    var accountNumber = "1111";
    var name = "John Doe";
    var userId = UUID.randomUUID().toString();
    var cardId = UUID.randomUUID().toString();
    var accountId = UUID.randomUUID().toString();
    var transactionId = UUID.randomUUID().toString();

    var initialBalance = 10000;
    var amountToWithdraw = 100;

    log.info("Creating account...");

    execute(componentClient.forAction().call(AccountController::create).params(accountId,new CreateAccountRequest(accountNumber,initialBalance)));

    log.info("Creating user...");
    execute(componentClient.forAction().call(UserController::create).params(userId,new CreateUserRequest(name,cardId,accountId)));

    await()
    .ignoreExceptions()
    .atMost(10, TimeUnit.SECONDS)
    .untilAsserted(() -> {
      var userByCardIdRes = execute(componentClient.forAction().call(UserController::getUserByCard).params(cardId));
      assertEquals(userId, userByCardIdRes.userId());
    });

    log.info("Initiating withdraw...");
    execute(componentClient.forAction().call(TransactionController::process).params(transactionId,new TransactionProcessRequest(amountToWithdraw,cardId)));


    await()
    .ignoreExceptions()
    .atMost(10, TimeUnit.SECONDS)
    .untilAsserted(() -> {
      var transactionByStatusViewRecordList = execute(componentClient.forAction().call(TransactionController::getTransactionsByStatus).params(DomainModel.TransactionStatus.SUCCESS.name()));
      assertEquals(1,transactionByStatusViewRecordList.list().size());
    });

  }

  @Test
  public void testAccountNotFound() throws Exception {
    var accountNumber = "1111";
    var name = "John Doe";
    var userId = UUID.randomUUID().toString();
    var cardId = UUID.randomUUID().toString();
    var accountId = UUID.randomUUID().toString();
    var transactionId = UUID.randomUUID().toString();

    var initialBalance = 10000;
    var amountToWithdraw = 100;

    log.info("Creating account...");
    execute(componentClient.forAction().call(AccountController::create).params(accountId,new CreateAccountRequest(accountNumber,initialBalance)));

    log.info("Creating user with wrong account...");
    accountId = "Wrong account";
    execute(componentClient.forAction().call(UserController::create).params(userId,new CreateUserRequest(name,cardId,accountId)));

    await()
    .ignoreExceptions()
    .atMost(10, TimeUnit.SECONDS)
    .untilAsserted(() -> {
      var userByCardIdRes = execute(componentClient.forAction().call(UserController::getUserByCard).params(cardId));
      assertEquals(userId, userByCardIdRes.userId());
    });

    log.info("Initiating withdraw...");
    execute(componentClient.forAction().call(TransactionController::process).params(transactionId,new TransactionProcessRequest(amountToWithdraw,cardId)));


    await()
    .ignoreExceptions()
    .atMost(10, TimeUnit.SECONDS)
    .untilAsserted(() -> {
      var transactionByStatusViewRecordList = execute(componentClient.forAction().call(TransactionController::getTransactionsByStatus).params(DomainModel.TransactionStatus.ACCOUNT_NOT_FOUND.name()));
      assertEquals(1, transactionByStatusViewRecordList.list().size());
    });

  }

  @Test
  public void testUserNotFound() throws Exception {
    var accountNumber = "1111";
    var name = "John Doe";
    var userId = UUID.randomUUID().toString();
    var cardId = UUID.randomUUID().toString();
    var accountId = UUID.randomUUID().toString();
    var transactionId = UUID.randomUUID().toString();

    var initialBalance = 10000;
    var amountToWithdraw = 100;

    log.info("Creating account...");
    execute(componentClient.forAction().call(AccountController::create).params(accountId,new CreateAccountRequest(accountNumber,initialBalance)));

    log.info("Creating user...");
    execute(componentClient.forAction().call(UserController::create).params(userId,new CreateUserRequest(name,cardId,accountId)));

    await()
    .ignoreExceptions()
    .atMost(10, TimeUnit.SECONDS)
    .untilAsserted(() -> {
      var userByCardIdRes = execute(componentClient.forAction().call(UserController::getUserByCard).params(cardId));
      assertEquals(userId, userByCardIdRes.userId());
    });

    log.info("Initiating withdraw with unknown card id...");
    var wrongCardId = "Wrong card";
    execute(componentClient.forAction().call(TransactionController::process).params(transactionId,new TransactionProcessRequest(amountToWithdraw,wrongCardId)));

    await()
    .ignoreExceptions()
    .atMost(10, TimeUnit.SECONDS)
    .untilAsserted(() -> {
      var transactionByStatusViewRecordList = execute(componentClient.forAction().call(TransactionController::getTransactionsByStatus).params(DomainModel.TransactionStatus.USER_NOT_FOUND.name()));
      assertEquals(1, transactionByStatusViewRecordList.list().size());
    });

  }

  @Test
  public void testFundsUnavailable() throws Exception {
    var accountNumber = "1111";
    var name = "John Doe";
    var userId = UUID.randomUUID().toString();
    var cardId = UUID.randomUUID().toString();
    var accountId = UUID.randomUUID().toString();
    var transactionId = UUID.randomUUID().toString();

    var initialBalance = 10000;
    var amountToWithdraw = 100;

    log.info("Creating account with low initial balance...");
    execute(componentClient.forAction().call(AccountController::create).params(accountId,new CreateAccountRequest(accountNumber,initialBalance)));

    log.info("Creating user...");
    execute(componentClient.forAction().call(UserController::create).params(userId,new CreateUserRequest(name,cardId,accountId)));

    await()
    .ignoreExceptions()
    .atMost(10, TimeUnit.SECONDS)
    .untilAsserted(() -> {
      var userByCardIdRes = execute(componentClient.forAction().call(UserController::getUserByCard).params(cardId));
      assertEquals(userId, userByCardIdRes.userId());
    });

    log.info("Initiating withdraw higher then initial balance...");
    amountToWithdraw = initialBalance + 100;
    execute(componentClient.forAction().call(TransactionController::process).params(transactionId,new TransactionProcessRequest(amountToWithdraw,cardId)));

    await()
    .ignoreExceptions()
    .atMost(10, TimeUnit.SECONDS)
    .untilAsserted(() -> {
      var transactionByStatusViewRecordList = execute(componentClient.forAction().call(TransactionController::getTransactionsByStatus).params(DomainModel.TransactionStatus.FUNDS_UNAVAILABLE.name()));
      assertEquals(1, transactionByStatusViewRecordList.list().size());
    });

  }

}