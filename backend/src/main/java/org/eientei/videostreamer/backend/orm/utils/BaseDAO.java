package org.eientei.videostreamer.backend.orm.utils;

import com.googlecode.genericdao.dao.hibernate.GenericDAOImpl;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.io.Serializable;

/**
 * User: iamtakingiteasy
 * Date: 2014-09-14
 * Time: 12:42
 */
@SuppressWarnings("SpringJavaAutowiredMembersInspection")
public class BaseDAO<T, ID extends Serializable> extends GenericDAOImpl<T, ID> {
    @Autowired
    private SessionFactory sessionFactory;

    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    @PostConstruct
    public void init() {
        setSessionFactory(sessionFactory);
    }
}
