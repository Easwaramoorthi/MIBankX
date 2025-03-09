package com.mi.bankx.service;

import com.mi.bankx.constants.MIBankXConstants;
import com.mi.bankx.model.*;
import com.mi.bankx.repository.*;

import lombok.extern.log4j.Log4j2;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Log4j2
public class BankingService {

	@Autowired
	private CustomerRepository customerRepository;

	@Autowired
	private AccountRepository accountRepository;

	@Autowired
	private TransactionRepository transactionRepository;

	@Autowired
	private NotificationRepository notificationRepository;

	/**
	 * Onboard a new customer with Savings and Current accounts. Credit 500 Rs in
	 * Savings Account by Default while creating New Account.
	 */
	@Transactional
	public Customer onboardCustomer(String name) {
		Customer customer = new Customer();
		customer.setName(name);

		customer = customerRepository.save(customer);

		Account savingsAccount = new Account(null, AccountType.SAVINGS, new BigDecimal("500.00"), customer);
		Account currentAccount = new Account(null, AccountType.CURRENT, BigDecimal.ZERO, customer);

		accountRepository.save(savingsAccount);
		accountRepository.save(currentAccount);

		log.info("New Customer {} Onboarded Successfully with Customer ID {}", customer.getName(), customer.getId());

		return customer;
	}

	/**
	 * Make payment from Current Account to external account.
	 */
	@Transactional
	public String makePayment(Long customerId, BigDecimal amount) {
		Optional<Customer> optionalCustomer = customerRepository.findById(customerId);
		if (!optionalCustomer.isPresent()) {
			return MIBankXConstants.CUSTNOTFOUND;
		}

		Customer customer = optionalCustomer.get();
		Account currentAccount = getAccountByType(customer, AccountType.CURRENT);

		BigDecimal transactionFee = amount.multiply(new BigDecimal("0.0005"));

		if (currentAccount.getBalance().compareTo(amount.add(transactionFee)) < 0) {
			return "Insufficient balance in Current Account";
		}

		// Deduct Amount + Transaction Fee
		currentAccount.setBalance(currentAccount.getBalance().subtract(amount.add(transactionFee)));
		accountRepository.save(currentAccount);

		recordTransaction(customer, "Payment", amount, transactionFee);
		sendNotification(customer, "Payment of " + amount + " processed with fee: " + transactionFee);

		log.info("Customer {} Made payment for the amount Rs: {}", customer.getId(), amount);
		return "Payment Successful for the Amount : " + amount + " processed with fee: " + transactionFee
				+ " Available Balance in your Current account is Rs: " + currentAccount.getBalance();
	}

	/**
	 * Apply interest (0.5%) on Savings Account deposits.
	 */
	@Transactional
	public void applyInterest(Long customerId) {
		Optional<Customer> optionalCustomer = customerRepository.findById(customerId);
		if (!optionalCustomer.isPresent()) {
			return;
		}

		Customer customer = optionalCustomer.get();
		Account savingsAccount = getAccountByType(customer, AccountType.SAVINGS);

		BigDecimal interest = savingsAccount.getBalance().multiply(new BigDecimal("0.005"));
		savingsAccount.setBalance(savingsAccount.getBalance().add(interest));

		accountRepository.save(savingsAccount);
		recordTransaction(customer, "Interest Applied", interest, BigDecimal.ZERO);
		sendNotification(customer, "Interest of " + interest + " credited to Savings Account");
	}

	/**
	 * Add money to the Current Account for a customer.
	 */
	@Transactional
	public String addMoneyToCurrent(Long customerId, BigDecimal amount) {
		Optional<Customer> optionalCustomer = customerRepository.findById(customerId);
		if (!optionalCustomer.isPresent()) {
			return MIBankXConstants.CUSTNOTFOUND;
		}

		Customer customer = optionalCustomer.get();
		Account currentAccount = getAccountByType(customer, AccountType.CURRENT);

		// Add money to Current Account
		currentAccount.setBalance(currentAccount.getBalance().add(amount));
		accountRepository.save(currentAccount);

		recordTransaction(customer, "Deposit to Current Account", amount, BigDecimal.ZERO);
		sendNotification(customer, "Deposited " + amount + " to your Current Account");

		log.info("Customer {} Deposited Amount Rs: {} to their Current Account", customer.getId(), amount);

		return amount + " Deposited to your Current Account. Your Updated Current Account Balance is Rs: "
				+ currentAccount.getBalance();
	}

	/**
	 * Transfer money from Current Account to Savings Account with 0.5% interest.
	 */
	@Transactional
	public String transferToSavings(Long customerId, BigDecimal amount) {
		Optional<Customer> optionalCustomer = customerRepository.findById(customerId);
		if (!optionalCustomer.isPresent()) {
			return MIBankXConstants.CUSTNOTFOUND;
		}

		Customer customer = optionalCustomer.get();
		Account currentAccount = getAccountByType(customer, AccountType.CURRENT);
		Account savingsAccount = getAccountByType(customer, AccountType.SAVINGS);

		if (currentAccount.getBalance().compareTo(amount) < 0) {
			return "Insufficient balance in Current Account";
		}

		// Calculate 0.5% interest on the transferred amount
		BigDecimal interest = amount.multiply(new BigDecimal("0.005")); // 0.5% of amount
		BigDecimal totalAmountToSavings = amount.add(interest); // Amount + Interest

		// Perform Transfer
		currentAccount.setBalance(currentAccount.getBalance().subtract(amount));
		savingsAccount.setBalance(savingsAccount.getBalance().add(totalAmountToSavings));

		accountRepository.save(currentAccount);
		accountRepository.save(savingsAccount);

		recordTransaction(customer, "Transfer to Savings", totalAmountToSavings, BigDecimal.ZERO);
		sendNotification(customer, "Transferred " + amount + " to Savings Account with 0.5% interest. Total credited: "
				+ totalAmountToSavings);

		log.info(
				"Customer {} Transfered Amount Rs: {} from their current Account to Savings Account, with an Interest of {}% added",
				customer.getId(), amount, interest);

		return amount
				+ " Rs Transfered Successfully with Interest to the Savings Account. Updated Savings Account Balance is Rs: "
				+ savingsAccount.getBalance();
	}

	/**
	 * Transfer money from one customer's Current Account to another customer's
	 * Current Account. A transaction fee of 0.05% applies to the transferred
	 * amount.
	 */
	@Transactional
	public String transferToAnotherCustomer(Long senderId, Long receiverId, BigDecimal amount) {
		Optional<Customer> senderOptional = customerRepository.findById(senderId);
		Optional<Customer> receiverOptional = customerRepository.findById(receiverId);

		if (!senderOptional.isPresent() || !receiverOptional.isPresent()) {
			return "Sender or Receiver not found";
		}

		Customer sender = senderOptional.get();
		Customer receiver = receiverOptional.get();

		Account senderCurrentAccount = getAccountByType(sender, AccountType.CURRENT);
		Account receiverCurrentAccount = getAccountByType(receiver, AccountType.CURRENT);

		// Calculate Transaction Fee (0.05% of the amount)
		BigDecimal transactionFee = amount.multiply(new BigDecimal("0.0005")); // 0.05% fee
		BigDecimal totalDebitAmount = amount.add(transactionFee); // Amount + Fee

		// Check if sender has enough balance
		if (senderCurrentAccount.getBalance().compareTo(totalDebitAmount) < 0) {
			return "Insufficient balance in Sender's Current Account";
		}

		// Perform Transfer
		senderCurrentAccount.setBalance(senderCurrentAccount.getBalance().subtract(totalDebitAmount));
		receiverCurrentAccount.setBalance(receiverCurrentAccount.getBalance().add(amount));

		accountRepository.save(senderCurrentAccount);
		accountRepository.save(receiverCurrentAccount);

		recordTransaction(sender, "Transfer to " + receiver.getName(), amount, transactionFee);
		recordTransaction(receiver, "Received from " + sender.getName(), amount, BigDecimal.ZERO);

		sendNotification(sender,
				"Transferred " + amount + " to " + receiver.getName() + " with a fee of " + transactionFee);
		sendNotification(receiver, "Received " + amount + " from " + sender.getName());

		log.info("Customer {} Transfered amount of Rs: {} to customer {} with a Transaction fee of Rs: {}",
				sender.getId(), amount, receiver.getId(), transactionFee);

		return amount + " Rs Successfully Transferred to Customer " + receiverId + " With a Tranaction Fee of Rs : "
				+ transactionFee + "Your Updated Current Account Balance after the Tranaction is Rs: "
				+ senderCurrentAccount.getBalance();
	}

	/**
	 * Retrieve all transactions for a customer.
	 */
	public List<Transaction> getTransactions(Long customerId) {
		return transactionRepository.findByCustomerId(customerId);
	}

	/**
	 * Retrieve all notifications for a customer.
	 */
	public List<Notification> getNotifications(Long customerId) {
		return notificationRepository.findByCustomerId(customerId);
	}

	/**
	 * Delete a customer and their accounts
	 */
	@Transactional
	public void deleteCustomer(Long customerId) {
		transactionRepository.deleteById(customerId);
		notificationRepository.deleteById(customerId);
		accountRepository.deleteById(customerId);
		customerRepository.deleteById(customerId);
	}

	/**
	 * Helper method to get account by type.
	 */
	private Account getAccountByType(Customer customer, AccountType type) {
		return customer.getAccounts().stream().filter(account -> account.getType() == type).findFirst()
				.orElseThrow(() -> new RuntimeException("Account not found"));
	}

	/**
	 * Helper method to record transactions.
	 */
	private void recordTransaction(Customer customer, String type, BigDecimal amount, BigDecimal fee) {
		Transaction transaction = new Transaction(null, customer, type, amount, fee, LocalDateTime.now());
		transactionRepository.save(transaction);
	}

	/**
	 * Helper method to send notifications.
	 */
	private void sendNotification(Customer customer, String message) {
		Notification notification = new Notification(null, customer, message, LocalDateTime.now());
		notificationRepository.save(notification);
	}
}