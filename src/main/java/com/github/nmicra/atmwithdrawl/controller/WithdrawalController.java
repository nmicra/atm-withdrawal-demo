package com.github.nmicra.atmwithdrawl.controller;


import com.github.nmicra.atmwithdrawl.service.WithdrawalService;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/withdrawal")
public class WithdrawalController {

    //////////////////////////////////////////////
    // http://localhost:8080/swagger-ui/index.html
    ///////////////////////////////////////////////

    @Autowired
    private WithdrawalService withdrawalService;
    private record WithdrawalRequestDto(String cardId, String code, Double amount){};

    @PostMapping("/withdraw")
    ResponseEntity withdraw(@RequestBody WithdrawalRequestDto dto){

        Pair<Boolean, String> booleanOptionalPair = withdrawalService.withdrawalAllowed(dto.cardId, dto.amount);
        if (booleanOptionalPair.getLeft()) {
            return ResponseEntity.ok().body(booleanOptionalPair.getRight());
        }
        return ResponseEntity.badRequest().body(booleanOptionalPair.getRight());
    }

    @PostMapping("/cancel/{cardId}")
    ResponseEntity cancelWithdraw(@PathVariable String cardId){

        Pair<Boolean, String> booleanOptionalPair = withdrawalService.cancelWithdrawal(cardId);
        if (booleanOptionalPair.getLeft()) {
            return ResponseEntity.ok().body(booleanOptionalPair.getRight());
        }
        return ResponseEntity.badRequest().body(booleanOptionalPair.getRight());
    }
}
