package org.eientei.video.orm.service;

import com.googlecode.genericdao.search.Search;
import org.eientei.video.orm.dao.MessageDAO;
import org.eientei.video.orm.entity.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * User: iamtakingiteasy
 * Date: 2014-09-15
 * Time: 17:48
 */
@Service
@Transactional(readOnly = true)
public class MessageService {
    @Autowired
    private MessageDAO dao;

    @Transactional(readOnly = false)
    public void storeMessage(Message message) {
        dao.save(message);
    }

    public List<Message> loadLastBackMessages(int total, int lowestId, String app, String name) {
        Search search = new Search();
        search.addFilterEqual("app", app);
        search.addFilterEqual("name", name);
        if (lowestId > 0) {
            search.addFilterLessThan("id", lowestId);
        }
        search.addSortDesc("id");
        search.setMaxResults(total);
        return dao.search(search);
    }

    public Message loadById(int id) {
        return dao.find(id);
    }
}
