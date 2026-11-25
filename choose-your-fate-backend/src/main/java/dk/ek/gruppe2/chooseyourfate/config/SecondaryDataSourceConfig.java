package dk.ek.gruppe2.chooseyourfate.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

/**
 * We are creating our own connection to the secondary database here.
 */
@Configuration
public class SecondaryDataSourceConfig {

    private final String url;
    private final String username;
    private final String password;

    public SecondaryDataSourceConfig(@Value("${app.datasource.secondary.url}") String url, @Value("${app.datasource.secondary.username}") String username, @Value("${app.datasource.secondary.password}") String password) {
        this.url = url;
        this.username = username;
        this.password = password;
    }

    @Bean(name = "secondaryDataSource")
    public DataSource secondaryDataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        return dataSource;
    }

    @Bean(name = "secondaryJdbcTemplate")
    public JdbcTemplate secondaryJdbcTemplate(@Qualifier("secondaryDataSource") DataSource secondaryDataSource) {
        return new JdbcTemplate(secondaryDataSource);
    }
}
