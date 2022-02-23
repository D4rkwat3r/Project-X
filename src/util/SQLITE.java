package util;

import lib.Credentials;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SQLITE {

    private Connection connection;
    private Statement statement;

    public SQLITE() {
        try {
            Class.forName("org.sqlite.JDBC");
            this.connection = DriverManager.getConnection("jdbc:sqlite:ProjX.db");
            this.statement = this.connection.createStatement();
            this.statement.execute("CREATE TABLE IF NOT EXISTS 'accounts' ('email' TEXT, 'password' TEXT, 'last_session' TEXT);");
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void writeAccount(Credentials credentials) {
        try {
            statement.execute(
                    String.format(
                            "INSERT INTO 'accounts' ('email', 'password', 'last_session') VALUES ('%s', '%s', '%s')",
                            credentials.getEmail(),
                            credentials.getPassword(),
                            credentials.getSession()
                    )
            );
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public List<Credentials> readAccounts() throws SQLException {
        List<Credentials> credentialsList = new ArrayList<>();
        ResultSet result = statement.executeQuery("SELECT * FROM accounts");
        while (result.next()) {
            credentialsList.add(
                            new Credentials()
                                    .setEmail(result.getString("email"))
                                    .setPassword(result.getString("password"))
                    );
        }
        return credentialsList;
    }
}
