package org.eientei.video.backend.orm.service;

import com.googlecode.genericdao.search.Search;
import org.eientei.video.backend.orm.dao.MessageDAO;
import org.eientei.video.backend.orm.entity.Message;
import org.eientei.video.backend.orm.entity.Stream;
import org.eientei.video.backend.orm.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * User: iamtakingiteasy
 * Date: 2015-05-07
 * Time: 19:40
 */
@Service
@Transactional(readOnly = true)
public class MessageService {
    @Autowired
    private MessageDAO messageDAO;

    public Message persist(Stream stream, String text, User user, boolean owner, String remote) {
        Message message = new Message();
        message.setAuthor(user);
        message.setIsAdmin(false);
        message.setIsAuthor(owner);
        message.setMessage(text);
        message.setStream(stream);
        message.setRemote(remote);
        messageDAO.save(message);
        return message;
    }

    public List<Message> last(Stream stream, int n) {
        Search search = new Search();
        search.addFilterEqual("stream", stream);
        search.addSortDesc("id");
        search.setMaxResults(n);
        return messageDAO.search(search);
    }

    public List<Message> from(Stream stream, int n, long id) {
        Search search = new Search();
        search.addFilterEqual("stream", stream);
        search.addFilterLessThan("id", id);
        search.addSortDesc("id");
        search.setMaxResults(n);
        return messageDAO.search(search);
    }

    public Message load(Stream stream, Long id) {
        Search search = new Search();
        search.addFilterEqual("stream", stream);
        search.addFilterEqual("id", id);
        return messageDAO.searchUnique(search);
    }
}
