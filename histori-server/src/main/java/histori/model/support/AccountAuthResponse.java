package histori.model.support;

import histori.model.Account;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.auth.AuthResponse;

@NoArgsConstructor @Slf4j
public class AccountAuthResponse extends AuthResponse<Account> {

    public static final AccountAuthResponse TWO_FACTOR = new AccountAuthResponse(true);

    private AccountAuthResponse(boolean twoFactor) { setSessionId(TWO_FACTOR_SID); }

    public AccountAuthResponse(String sessionId, Account account) { super(sessionId, account); }

}
