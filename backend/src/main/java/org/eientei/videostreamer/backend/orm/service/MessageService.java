package org.eientei.videostreamer.backend.orm.service;

import com.googlecode.genericdao.search.Search;
import org.eientei.videostreamer.backend.orm.dao.MessageDAO;
import org.eientei.videostreamer.backend.orm.entity.Message;
import org.eientei.videostreamer.backend.orm.entity.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * User: iamtakingiteasy
 * Date: 2014-12-21
 * Time: 00:15
 */
@Service
@Transactional(readOnly = true)
public class MessageService {
    @Autowired
    private MessageDAO dao;

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

    public Message loadById(long id) {
        return dao.find(id);
    }

    public int getTotalMessages(String app, String name) {
        Search search = new Search();
        search.addFilterEqual("app", app);
        search.addFilterEqual("name", name);
        return dao.count(search);
    }

    public List<Message> loadLast(Stream stream, long id, int bulkSize) {
        Search search = new Search();
        search.addFilterEqual("stream", stream);

        if (id > 0) {
            search.addFilterLessThan("id", id);
        }

        search.addSortDesc("id");
        search.setMaxResults(bulkSize);
        return dao.search(search);
    }

    @Transactional(readOnly = false)
    public void saveMessage(Message message) {
        dao.save(message);
    }

}
