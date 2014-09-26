package org.eientei.video.config;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.hibernate4.HibernateTransactionManager;
import org.springframework.orm.hibernate4.LocalSessionFactoryBean;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.util.Properties;

/**
 * User: iamtakingiteasy
 * Date: 2014-09-13
 * Time: 15:39
 */
@EnableTransactionManagement
@Configuration
@ComponentScan(basePackages = "org.eientei.video.orm")
public class PersistenceAppConfig {
    @Bean
    public DataSource getDataSource() throws NamingException {
        Context context = new InitialContext();
        //context = (Context)context.lookup("java:comp/env");
        return (DataSource) context.lookup("jdbc/VideoDS");
    }

    @Autowired
    @Bean
    public LocalSessionFactoryBean getSessionFactory(DataSource dataSource) throws Exception {
        Context context = new InitialContext();
        //context = (Context)context.lookup("java:comp/env");

        String dialect = (String) context.lookup("jdbc/VideoHibernateDialect");
        String hbmToDdl;

        try {
            hbmToDdl = (String) context.lookup("jdbc/VideoHibernateToDdl");
        } catch (Exception e) {
            hbmToDdl = "update";
        }

        Properties props = new Properties();
        props.put("hibernate.dialect", dialect);
        props.put("hibernate.hbm2ddl.auto", hbmToDdl);
        props.put("hibernate.show_sql", false);
        props.put("hibernate.format_sql", false);

        LocalSessionFactoryBean sessionFactory = new LocalSessionFactoryBean();
        sessionFactory.setDataSource(dataSource);
        sessionFactory.setPackagesToScan("org.eientei.video.orm.entity");
        sessionFactory.setHibernateProperties(props);
        return sessionFactory;
    }

    @Autowired
    @Bean
    public HibernateTransactionManager getTransactionManager(SessionFactory sessionFactory) {
        return new HibernateTransactionManager(sessionFactory);
    }
}
