package dk.ek.gruppe2.chooseyourfate.controller;

import org.springframework.web.bind.annotation.*;

import dk.ek.gruppe2.chooseyourfate.dto.AccountResponseDTO;
import dk.ek.gruppe2.chooseyourfate.dto.AuthTokenResponseDTO;
import dk.ek.gruppe2.chooseyourfate.dto.CreateAccountRequestDTO;
import dk.ek.gruppe2.chooseyourfate.dto.LoginDTO;
import dk.ek.gruppe2.chooseyourfate.security.JwtUtil;
import dk.ek.gruppe2.chooseyourfate.service.AccountService;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

@RestController
@RequestMapping("/auth")
public class AuthenticationController {

    private final AuthenticationManager authManager;
    private final JwtUtil jwtUtil;
    private final AccountService accountService;

    public AuthenticationController(AuthenticationManager authManager,
                                    JwtUtil jwtUtil,
                                    AccountService accountService) {
        this.authManager = authManager;
        this.jwtUtil = jwtUtil;
        this.accountService = accountService;
    }

    @PostMapping("/register")
    public AccountResponseDTO register(@RequestBody CreateAccountRequestDTO acc) {
        return accountService.registerAccount(acc);
    }

    @PostMapping("/login")
    public AuthTokenResponseDTO login(@RequestBody LoginDTO request) {

        Authentication auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        UserDetails user = (UserDetails) auth.getPrincipal();
        return new AuthTokenResponseDTO(jwtUtil.generateToken(user));
    }
}