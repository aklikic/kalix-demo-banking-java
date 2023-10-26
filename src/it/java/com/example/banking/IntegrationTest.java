package com.example.banking;

import com.example.banking.transaction.DomainModel;
import kalix.spring.testkit.KalixIntegrationTestKitSupport;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.example.banking.account.AccountApiModel.CreateAccountRequest;
import static com.example.banking.transaction.TransactionApiModel.TransactionProcessRequest;
import static com.example.banking.user.UserApiModel.CreateUserRequest;
import static org.junit.jupiter.api.Assertions.assertEquals;


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

  private static final Logger log = LoggerFactory.getLogger(IntegrationTest.class);

  @Test
  public void test() throws Exception {
    var accountNumber = "1111";
    var name = "John Doe";
    var userId = UUID.randomUUID().toString();
    var cardId = UUID.randomUUID().toString();
    var accountId = UUID.randomUUID().toString();
    var transactionId = UUID.randomUUID().toString();

    var initialBalance = 10000;
    var amountToWithdraw = 100;

    log.info("Creating account...");
    componentClient.forAction().call(BankingApiController::createAccount).params(accountId,new CreateAccountRequest(accountNumber,initialBalance)).execute().toCompletableFuture().get(3, TimeUnit.SECONDS);

    log.info("Creating user...");
    componentClient.forAction().call(BankingApiController::createUser).params(userId,new CreateUserRequest(name,cardId,accountId)).execute().toCompletableFuture().get(3, TimeUnit.SECONDS);

    Thread.sleep(3000);
    var userByCardIdRes = componentClient.forAction().call(BankingApiController::getUserByCard).params(cardId).execute().toCompletableFuture().get(3, TimeUnit.SECONDS);
    assertEquals(userId,userByCardIdRes.userId());

    log.info("Initiating withdraw...");
    componentClient.forAction().call(BankingApiController::processTransaction).params(transactionId,new TransactionProcessRequest(amountToWithdraw,cardId)).execute().toCompletableFuture().get(3, TimeUnit.SECONDS);;

    Thread.sleep(5000);

    var transactionByStatusViewRecordList = componentClient.forAction().call(BankingApiController::getTransactionsByStatus).params(DomainModel.TransactionStatus.PROCESSED_SUCCESS.name()).execute().toCompletableFuture().get(10, TimeUnit.SECONDS);
    assertEquals(1,transactionByStatusViewRecordList.list().size());

  }
}