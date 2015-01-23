package org.eientei.videostreamer.backend.orm.service;

import com.googlecode.genericdao.search.Search;
import org.eientei.videostreamer.backend.orm.dao.StreamDAO;
import org.eientei.videostreamer.backend.orm.entity.Stream;
import org.eientei.videostreamer.backend.orm.entity.User;
import org.eientei.videostreamer.backend.utils.VideostreamerUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * User: iamtakingiteasy
 * Date: 2014-12-20
 * Time: 21:54
 */
@Service
@Transactional(readOnly = true)
public class StreamService {
    public static class StreamExists extends RuntimeException {
        public StreamExists() {
            super("Stream with such name already exists");
        }
    }

    public static class StreamNotExists extends RuntimeException {
        public StreamNotExists() {
            super("Stream not exists");
        }
    }

    public static class StreamNotOwned extends RuntimeException {
        public StreamNotOwned() {
            super("Stream not owned");
        }
    }

    public static class StreamInvalidName extends RuntimeException {
        public StreamInvalidName() {
            super("Stream name invalid");
        }
    }

    public static class StreamExhaustion extends RuntimeException {
        public StreamExhaustion() {
            super("Stream limit exhausted");
        }
    }

    @Autowired
    private StreamDAO dao;

    @Autowired
    private UserService userService;

    public List<Stream> getActiveStreams() {
        Search search = new Search();
        search.addFilterNotNull("remote");
        return dao.search(search);
    }

    public Stream getStream(String app, String name) {
        Search search = new Search();
        search.addFilterCustom("lower(cast({app} as text)) = lower(cast(?1 as text))", app);
        search.addFilterCustom("lower(cast({name} as text)) = lower(cast(?1 as text))", name);
        return dao.searchUnique(search);
    }

    @Transactional(readOnly = false)
    public Stream allocateStream(User user, String app) {
        Stream stream = new Stream();
        stream.setApp(app);
        stream.setName(user.getName());
        stream.setAuthor(user);
        stream.setTopic("Changeme");
        stream.setIdleImage(null);
        stream.setToken(VideostreamerUtils.hashMd5(UUID.randomUUID().toString()));
        stream.setRestricted(false);

        int idx = 0;

        while (true) {
            if (getStream(app, stream.getName()) != null) {
                stream.setName(user.getName() + idx);
                idx++;
                continue;
            }

            try {
                dao.save(stream);
                break;
            } catch (Throwable t) {
            }
        }

        user.getStreams().add(stream);
        return stream;
    }


    public List<Stream> getUserStreams(User dataUser) {
        Search search = new Search();
        search.addFilterEqual("author", dataUser);
        search.addSort("id", false);
        return dao.search(search);
    }

    @Transactional(readOnly = false)
    public void updateStreamTopic(Stream stream, String streamTopic) {
        stream.setTopic(streamTopic);
        dao.save(stream);
    }

    @Transactional(readOnly = false)
    public void updateStreamKey(User dataUser, long id) {
        Stream stream = geStreamByIdForUser(id, dataUser);

        stream.setToken(VideostreamerUtils.hashMd5(UUID.randomUUID().toString()));
        while (true) {
            try {
                dao.save(stream);
                break;
            } catch (Exception e) {
                stream.setToken(VideostreamerUtils.hashMd5(UUID.randomUUID().toString()));
            }
        }
    }

    @Transactional(readOnly = false)
    public void updateStreamName(User dataUser, long id, String name) {
        if (name == null || name.length() < 3 || name.length() > 64 || !name.matches("^[a-zA-Z][0-9a-zA-Z_.-]*$")) {
            throw new StreamInvalidName();
        }

        Stream stream = geStreamByIdForUser(id, dataUser);
        if (stream.getAuthor().getId() != stream.getAuthor().getId()) {
            throw new StreamExists();
        }

        if (getStream(stream.getApp(), name) != null) {
            throw new StreamExists();
        }

        User otherUser = userService.getUserByNameLax(name);
        if (otherUser != null && stream.getAuthor().getId() != otherUser.getId()) {
            throw new StreamExists();
        }

        stream.setName(name);
        dao.save(stream);
    }

    @Transactional(readOnly = false)
    public Stream updateStreamTopic(User dataUser, long id, String topic) {
        Stream stream = geStreamByIdForUser(id, dataUser);
        stream.setTopic(topic);
        dao.save(stream);
        return stream;
    }

    @Transactional(readOnly = false)
    public void updateStreamImage(User dataUser, long id, String image) {
        Stream stream = geStreamByIdForUser(id, dataUser);
        stream.setIdleImage(image);
        dao.save(stream);
    }

    @Transactional(readOnly = false)
    public void updateStreamPrivate(User dataUser, long id) {
        Stream stream = geStreamByIdForUser(id, dataUser);
        stream.setRestricted(!stream.isRestricted());
        dao.save(stream);
    }

    @Transactional(readOnly = false)
    public void deleteStream(User dataUser, long id) {
        Stream stream = geStreamByIdForUser(id, dataUser);
        dataUser.getStreams().remove(stream);
        userService.saveUser(dataUser);
        dao.remove(stream);
    }

    private Stream geStreamByIdForUser(long id, User dataUser) {
        Stream stream = dao.find(id);
        if (stream == null) {
            throw new StreamNotExists();
        }
        if (stream.getAuthor().getId() != dataUser.getId()) {
            throw new StreamNotOwned();
        }
        return stream;
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

    public void refresh(Stream stream) {
        dao.refresh(stream);
    }
}
