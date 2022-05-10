package mathax.client.systems.accounts.types;

import mathax.client.systems.accounts.Account;
import mathax.client.systems.accounts.AccountType;
import mathax.client.systems.accounts.MicrosoftLogin;
import net.minecraft.client.util.Session;

public class MicrosoftAccount extends Account<MicrosoftAccount> {
    public MicrosoftAccount(String refreshToken) {
        super(AccountType.Microsoft, refreshToken);
    }

    @Override
    public boolean fetchInfo() {
        return auth() != null;
    }

    @Override
    public boolean login() {
        String token = auth();
        if (token == null) return false;

        setSession(new Session(cache.username, cache.uuid, token, "mojang"));
        return true;
    }

    private String auth() {
        MicrosoftLogin.LoginData data = MicrosoftLogin.login(name);
        if (!data.isGood()) return null;

        name = data.newRefreshToken;
        cache.username = data.username;
        cache.uuid = data.uuid;

        return data.mcToken;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof MicrosoftAccount)) return false;
        return ((MicrosoftAccount) o).name.equals(this.name);
    }
}
