package mathax.client.systems.accounts;

import mathax.client.systems.accounts.types.CrackedAccount;
import mathax.client.systems.accounts.types.MicrosoftAccount;
import mathax.client.systems.accounts.types.MojangAccount;
import mathax.client.systems.accounts.types.TheAlteningAccount;
import mathax.client.systems.System;
import mathax.client.systems.Systems;
import mathax.client.utils.misc.NbtException;
import mathax.client.utils.misc.NbtUtils;
import mathax.client.utils.network.MatHaxExecutor;
import net.minecraft.nbt.NbtCompound;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Accounts extends System<Accounts> implements Iterable<Account<?>> {
    private List<Account<?>> accounts = new ArrayList<>();

    public Accounts() {
        super("Accounts");
    }

    public static Accounts get() {
        return Systems.get(Accounts.class);
    }

    @Override
    public void init() {
        AccountCache.loadSteveHead();
    }

    public void add(Account<?> account) {
        accounts.add(account);
        save();
    }

    public boolean exists(Account<?> account) {
        return accounts.contains(account);
    }

    public void remove(Account<?> account) {
        if (accounts.remove(account)) save();
    }

    public int size() {
        return accounts.size();
    }

    @Override
    public Iterator<Account<?>> iterator() {
        return accounts.iterator();
    }

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();

        tag.put("accounts", NbtUtils.listToTag(accounts));

        return tag;
    }

    @Override
    public Accounts fromTag(NbtCompound tag) {
        MatHaxExecutor.execute(() -> accounts = NbtUtils.listFromTag(tag.getList("accounts", 10), tag1 -> {
            NbtCompound t = (NbtCompound) tag1;
            if (!t.contains("type")) return null;

            AccountType type = AccountType.valueOf(t.getString("type"));

            try {
                Account<?> account = switch (type) {
                    case Cracked ->    new CrackedAccount(null).fromTag(t);
                    case Microsoft ->  new MicrosoftAccount(null).fromTag(t);
                    case Mojang ->  new MojangAccount(null, null).fromTag(t);
                    case The_Altening -> new TheAlteningAccount(null).fromTag(t);
                };

                if (account.fetchHead()) return account;
            } catch (NbtException e) {
                return null;
            }

            return null;
        }));

        return this;
    }
}
