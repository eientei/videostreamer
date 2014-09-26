package org.eientei.video.orm.service;

import com.googlecode.genericdao.search.Search;
import org.eientei.video.orm.dao.StreamDAO;
import org.eientei.video.orm.entity.Stream;
import org.eientei.video.orm.entity.User;
import org.eientei.video.orm.util.VideostreamUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * User: iamtakingiteasy
 * Date: 2014-09-25
 * Time: 22:08
 */
@Service
@Transactional(readOnly = true)
public class StreamService {
    public static class StreamExists extends Exception { }
    public static class StreamInavlidName extends Exception {
        public StreamInavlidName(String s) {
            super(s);
        }
    }

    @Autowired
    private StreamDAO dao;

    public List<Stream> getActiveStreams() {
        Search search = new Search();
        search.addFilterNotNull("remote");
        return dao.search(search);
    }

    public Stream getStream(String app, String name) {
        Search search = new Search();
        search.addFilterEqual("app", app);
        search.addFilterEqual("name", name);
        return dao.searchUnique(search);
    }

    private String genToken() {
        return VideostreamUtils.hashMd5(UUID.randomUUID().toString());
    }

    @Transactional(readOnly = false)
    public Stream allocateStream(String app, String name, User user) throws StreamExists, StreamInavlidName {
        validateName(name);

        if (getStream(app, name) != null) {
            throw new StreamExists();
        }

        Stream stream = new Stream();
        stream.setApp(app);
        stream.setName(name);
        stream.setAuthor(user);
        stream.setTopic("Changeme");
        stream.setIdleImage("default");
        stream.setToken(genToken());

        while (true) {
            try {
                dao.save(stream);
                break;
            } catch (Exception e) {
                if (getStream(app, name) != null) {
                    throw new StreamExists();
                }
                stream.setToken(genToken());
            }
        }

        return stream;
    }

    private void validateName(String name) throws StreamInavlidName {
        if (!name.matches("^[0-9a-zA-Z_.-]{3,}$")) {
            throw new StreamInavlidName("Stream name must consist of >3 symbols in [0-9a-zA-Z_.-] range");
        }
    }


    public Stream getUserStream(User dataUser) {
        Search search = new Search();
        search.addFilterEqual("author", dataUser);
        search.addSort("id", false);
        return dao.searchUnique(search);
    }

    public int getUserStreamsCount(User dataUser) {
        Search search = new Search();
        search.addFilterEqual("author", dataUser);
        return dao.count(search);
    }

    public Stream getStreamByIdFor(long num, User dataUser) {
        Search search = new Search();
        search.addFilterEqual("id", num);
        search.addFilterEqual("author", dataUser);
        return dao.searchUnique(search);
    }

    @Transactional(readOnly = false)
    public void updateStreamName(long num, User dataUser, String newName) throws StreamExists, StreamInavlidName {
        validateName(newName);

        Stream stream = getStreamByIdFor(num, dataUser);
        if (stream != null) {
            if (getStream(stream.getApp(), newName) != null) {
                throw new StreamExists();
            }
            stream.setName(newName);
            dao.save(stream);
        }
    }

    @Transactional(readOnly = false)
    public void updateStreamToken(long id, User dataUser) {
        Stream stream = getStreamByIdFor(id, dataUser);
        if (stream != null) {
            stream.setToken(genToken());
            dao.save(stream);
            while (true) {
                try {
                    dao.save(stream);
                    break;
                } catch (Exception e) {
                    stream.setToken(genToken());
                }
            }
        }
    }

    @Transactional(readOnly = false)
    public void updateStreamTopic(String title, User dataUser) {
        Stream stream = getUserStream(dataUser);
        if (stream != null) {
            stream.setTopic(title);
            dao.save(stream);
        }
    }

    public Stream getStreamByToken(String token) {
        Search search = new Search();
        search.addFilterEqual("token", token);
        return dao.searchUnique(search);
    }

    @Transactional(readOnly = false)
    public void saveStream(Stream stream) {
        dao.save(stream);
    }
}
