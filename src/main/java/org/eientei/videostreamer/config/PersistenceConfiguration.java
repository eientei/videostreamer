package org.eientei.videostreamer.config;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.hibernate4.HibernateExceptionTranslator;

import javax.persistence.EntityManagerFactory;

/**
 * User: iamtakingiteasy
 * Date: 2015-05-05
 * Time: 18:56
 */
@Configuration
public class PersistenceConfiguration {
    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Bean
    public SessionFactory sessionFactory() {
        return entityManagerFactory.unwrap(SessionFactory.class);
    }

    @Bean
    public HibernateExceptionTranslator persistenceExceptionTranslationPostProcessor() {
        return new HibernateExceptionTranslator();
    }
}
