package com.mi.bankx.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.mi.bankx.model.Customer;
import com.mi.bankx.model.Notification;
import com.mi.bankx.model.Transaction;
import com.mi.bankx.service.BankingService;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api")
public class BankingController {

	private final BankingService bankingService;

	public BankingController(BankingService bankingService) {
		this.bankingService = bankingService;
	}

	@PostMapping("/customers")
	public ResponseEntity<Customer> createCustomer(@RequestParam String name) {
		Customer customer = bankingService.onboardCustomer(name);
		return ResponseEntity.ok(customer);
	}

	@PostMapping("/accounts/pay")
	public ResponseEntity<String> makePayment(@RequestParam Long customerId, @RequestParam BigDecimal amount) {
		String response = bankingService.makePayment(customerId, amount);
		return ResponseEntity.ok(response);
	}

	@GetMapping("/transactions/{customerId}")
	public ResponseEntity<List<Transaction>> getTransactionHistory(@PathVariable Long customerId) {
		List<Transaction> response = bankingService.getTransactions(customerId);
		return ResponseEntity.ok(response);
	}

	@GetMapping("/notifications/{customerId}")
	public ResponseEntity<List<Notification>> getNotifications(@PathVariable Long customerId) {
		List<Notification> response = bankingService.getNotifications(customerId);
		return ResponseEntity.ok(response);
	}

	/**
	 * Transfer money from Current Account to Savings Account with 0.5% interest.
	 */
	@PostMapping("/accounts/transferToSavings")
	public ResponseEntity<String> transferToSavings(@RequestParam Long customerId, @RequestParam BigDecimal amount) {
		String response = bankingService.transferToSavings(customerId, amount);
		return ResponseEntity.ok(response);
	}

	/**
	 * Transfer money between two customers' Current Accounts with a 0.05% fee.
	 */
	@PostMapping("/transferToCustomer")
	public ResponseEntity<String> transferToAnotherCustomer(@RequestParam Long senderId, @RequestParam Long receiverId,
			@RequestParam BigDecimal amount) {
		String response = bankingService.transferToAnotherCustomer(senderId, receiverId, amount);
		return ResponseEntity.ok(response);
	}

	/**
	 * Add money to a customer's Current Account.
	 */
	@PostMapping("/deposit")
	public ResponseEntity<String> addMoneyToCurrent(@RequestParam Long customerId, @RequestParam BigDecimal amount) {
		String response = bankingService.addMoneyToCurrent(customerId, amount);
		return ResponseEntity.ok(response);
	}
}