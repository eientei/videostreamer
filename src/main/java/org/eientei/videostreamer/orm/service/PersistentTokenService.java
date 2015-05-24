package org.eientei.videostreamer.orm.service;

import com.googlecode.genericdao.search.Search;
import org.eientei.videostreamer.orm.dao.PersistentTokenDAO;
import org.eientei.videostreamer.orm.entity.PersistentToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * User: iamtakingiteasy
 * Date: 2015-05-04
 * Time: 11:13
 */
@Service
@Transactional(readOnly = true)
public class PersistentTokenService {
    @Autowired
    private PersistentTokenDAO persistentTokenDAO;

    @Transactional(readOnly = false)
    public void create(String username, String series, String tokenValue, Date date) {
        PersistentToken token = new PersistentToken();
        token.setUsername(username);
        token.setSeries(series);
        token.setToken(tokenValue);
        token.setTimestamp(date);
        persistentTokenDAO.save(token);
    }

    @Transactional(readOnly = false)
    public void update(String series, String tokenValue, Date date) {
        PersistentToken token = persistentTokenDAO.find(series);
        if (token == null) {
            return;
        }
        token.setToken(tokenValue);
        token.setTimestamp(date);
        persistentTokenDAO.save(token);
    }

    public PersistentToken find(String series) {
        return persistentTokenDAO.find(series);
    }

    @Transactional(readOnly = false)
    public void deleteByUserName(String username) {
        Search search = new Search();
        search.addFilterEqual("username", username);
        List<PersistentToken> result = persistentTokenDAO.search(search);
        persistentTokenDAO.remove(result.toArray(new PersistentToken[result.size()]));
    }
}
