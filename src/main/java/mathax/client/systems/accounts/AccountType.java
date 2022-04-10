package mathax.client.systems.accounts;

public enum AccountType {
    Cracked("Cracked"),
    Microsoft("Microsoft"),
    Mojang("Mojang"),
    The_Altening("The Altening");

    private final String title;

    AccountType(String title) {
        this.title = title;
    }

    @Override
    public String toString() {
        return title;
    }
}
