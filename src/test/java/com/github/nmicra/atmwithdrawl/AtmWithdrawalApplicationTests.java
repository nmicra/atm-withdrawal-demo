package com.github.nmicra.atmwithdrawl;

import com.github.nmicra.atmwithdrawl.service.WithdrawalService;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class AtmWithdrawalApplicationTests {

	@Autowired
	private WithdrawalService withdrawalService;

	@Test
	@DisplayName("Simple positive test for withdrawal")
	void testSimpleAmount() {
		Pair<Boolean, String> resultPair = withdrawalService.withdrawalAllowed("123a", 20.0);
		assert(resultPair.getLeft());
	}


	@Test
	@DisplayName("Simple negative test, amount too big")
	void testNegativeAmountTooBig() {
		Pair<Boolean, String> resultPair = withdrawalService.withdrawalAllowed("123b", 21000.0);
		Assertions.assertTrue(!resultPair.getLeft());
		Assertions.assertEquals("amount 21000.0 is too big", resultPair.getRight());
	}

	@Test
	@DisplayName("Tests restriction on number of withdrawals")
	void testRestrictionOnNumberOfWithdrawals() {
		for (int i = 0; i < 5; i++) {
			Pair<Boolean, String> resultPair = withdrawalService.withdrawalAllowed("123c", 20.0);
			Assertions.assertTrue(resultPair.getLeft());
		}
		Pair<Boolean, String> resultPair = withdrawalService.withdrawalAllowed("123c", 20.0);
		Assertions.assertTrue(!resultPair.getLeft());
		Assertions.assertEquals("try again tomorrow. Allowed withdrawal times per day 5", resultPair.getRight());
	}

}
