package org.eientei.videostreamer.orm.dao;

import com.googlecode.genericdao.dao.hibernate.GenericDAOImpl;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.io.Serializable;

/**
 * User: iamtakingiteasy
 * Date: 2015-05-04
 * Time: 10:56
 */
@SuppressWarnings("SpringJavaAutowiredMembersInspection")
public class Base<T, ID extends Serializable> extends GenericDAOImpl<T,ID> {
    @Autowired
    private SessionFactory sessionFactory;

    @PostConstruct
    public void postConstruct() {
        setSessionFactory(sessionFactory);
    }
}
