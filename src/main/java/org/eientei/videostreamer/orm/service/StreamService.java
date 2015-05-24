package org.eientei.videostreamer.orm.service;

import com.googlecode.genericdao.search.Search;
import org.eientei.videostreamer.orm.dao.MessageDAO;
import org.eientei.videostreamer.orm.dao.StreamDAO;
import org.eientei.videostreamer.orm.entity.Message;
import org.eientei.videostreamer.orm.entity.Stream;
import org.eientei.videostreamer.orm.entity.User;
import org.eientei.videostreamer.orm.error.AlreadyExists;
import org.eientei.videostreamer.orm.error.TooManyStreams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.encoding.Md5PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * User: iamtakingiteasy
 * Date: 2015-05-04
 * Time: 14:12
 */
@Service
@Transactional(readOnly = true)
public class StreamService {
    @Autowired
    private StreamDAO streamDAO;

    @Autowired
    private UserService userService;

    @Autowired
    private MessageDAO messageDAO;

    @Autowired
    private Md5PasswordEncoder passwordEncoder;

    public Stream getStream(String app, String name) {
        Search search = new Search();
        search.addFilterCustom("lower(cast({app} as text)) = lower(cast(?1 as text))", app);
        search.addFilterCustom("lower(cast({name} as text)) = lower(cast(?1 as text))", name);
        return streamDAO.searchUnique(search);
    }

    public List<Stream> running() {
        Search search = new Search();
        search.addFilterNotNull("remote");
        search.addFilterEqual("restricted", false);
        return streamDAO.search(search);
    }

    @Transactional(readOnly = false)
    public void updateStreamTopic(Stream stream, String topic) {
        streamDAO.refresh(stream);
        stream.setTopic(topic);
        streamDAO.save(stream);
    }

    public List<Stream> belongs(User user) {
        Search search = new Search();
        search.addFilterEqual("author", user);
        search.addSortAsc("id");
        return streamDAO.search(search);
    }

    @Transactional(readOnly = false)
    public synchronized Stream allocateStream(User user) throws TooManyStreams {
        String name = user.getName();
        int idx = 1;

        Search search = new Search();
        search.addFilterEqual("author", user);
        if (streamDAO.count(search) >= 5) {
            throw new TooManyStreams();
        }

        while (getStream("live", name) != null || userService.getUserByUserName(name) != null) {
            name = user.getName() + "-" + idx++;
        }

        Stream stream = new Stream();
        stream.setApp("live");
        stream.setName(name);
        stream.setAuthor(user);
        stream.setTopic("changeme");
        stream.setIdleImage(null);
        stream.setToken(hashMd5(UUID.randomUUID().toString()));
        stream.setRestricted(false);
        streamDAO.save(stream);
        return stream;
    }

    @Transactional(readOnly = false)
    public void deallocate(User user, String app, String name) {
        Stream stream = forUser(user, app, name);

        Search messageSearch = new Search();
        messageSearch.addFilterEqual("stream", stream);

        List<Message> messages = messageDAO.search(messageSearch);
        messageDAO.remove(messages.toArray(new Message[messages.size()]));
        streamDAO.remove(stream);
    }

    @Transactional(readOnly = false)
    public void updateStreamName(User user, Stream stream, String newname) throws AlreadyExists {
        if (getStream("live", newname) != null || (userService.getUserByUserName(newname) != null && !user.getName().equalsIgnoreCase(newname))) {
            throw new AlreadyExists();
        }

        stream = load(stream);
        stream.setName(newname);
        streamDAO.save(stream);
    }

    private Stream load(Stream stream) {
        return streamDAO.find(stream.getId());
    }

    @Transactional(readOnly = false)
    public void updateStreamImage(Stream stream, String newurl) {
        stream = load(stream);
        stream.setIdleImage(newurl);
        streamDAO.save(stream);
    }

    @Transactional(readOnly = false)
    public String generateStreamToken(Stream stream) {
        stream = load(stream);
        String token = hashMd5(UUID.randomUUID().toString());
        stream.setToken(token);
        streamDAO.save(stream);
        return token;
    }

    @Transactional(readOnly = false)
    public void updateRemote(Stream stream, String addr) {
        stream = load(stream);
        stream.setSince(new Date());
        stream.setRemote(addr);
        streamDAO.save(stream);
    }

    @Transactional(readOnly = false)
    public void updateStreamPrivate(Stream stream, boolean restricted) {
        stream = load(stream);
        stream.setRestricted(restricted);
        streamDAO.save(stream);
    }

    public Stream forUser(User user, String app, String name) {
        Search search = new Search();
        search.addFilterEqual("author", user);
        search.addFilterCustom("lower(cast({app} as text)) = lower(cast(?1 as text))", app);
        search.addFilterCustom("lower(cast({name} as text)) = lower(cast(?1 as text))", name);
        return streamDAO.searchUnique(search);
    }

    private String hashMd5(String data) {
        return passwordEncoder.encodePassword(data, null);
    }

    public void refresh(Stream stream) {
        streamDAO.refresh(stream);
    }

    public Stream getStreamByToken(String token) {
        Search search = new Search();
        search.addFilterEqual("token", token);
        return streamDAO.searchUnique(search);
    }
}
