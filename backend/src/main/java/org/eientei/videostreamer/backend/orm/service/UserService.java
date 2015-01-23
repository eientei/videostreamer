package org.eientei.videostreamer.backend.orm.service;

import com.googlecode.genericdao.search.Search;
import org.eientei.videostreamer.backend.orm.dao.UserDAO;
import org.eientei.videostreamer.backend.orm.entity.User;
import org.eientei.videostreamer.backend.utils.VideostreamerUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * User: iamtakingiteasy
 * Date: 2014-12-20
 * Time: 21:47
 */
@Service
@Transactional(readOnly = true)
public class UserService {
    public static class UserExists extends RuntimeException {
        public UserExists() {
            super("User already exists");
        }
    }

    public static class UserInvalidName extends RuntimeException {
        public UserInvalidName() {
            super("User name invalid");
        }
    }

    public static class UserInvalidPassword extends RuntimeException {
        public UserInvalidPassword() {
            super("User password invalid");
        }
    }

    @Autowired
    private UserDAO userDao;

    @Autowired
    private StreamService streamService;

    public User getUserByNameLax(String name) {
        Search search = new Search();
        search.addFilterCustom("lower(cast({name} as text)) = lower(cast(?1 as text))", name);
        return userDao.searchUnique(search);
    }

    public User getUserByNameStrict(String name) {
        Search search = new Search();
        search.addFilterEqual("name", name);
        return userDao.searchUnique(search);
    }


    @Transactional(readOnly = false)
    public synchronized User createUser(String username, String password, String email) throws UserExists {
        if (username == null || username.length() < 3 || username.length() > 64 || !username.matches("^[a-zA-Z][0-9a-zA-Z_.-]*$")) {
            throw new UserInvalidName();
        }

        if (getUserByNameLax(username) != null) {
            throw new UserExists();
        }

        if (streamService.getStream("live", username) != null) {
            throw new UserExists();
        }

        User user = new User();
        user.setName(username);
        if (password != null) {
            user.setPasswordhash(VideostreamerUtils.hashMd5(password));
        }
        user.setEmail(email);
        try {
            userDao.save(user);
        } catch (Throwable t) {
            throw new UserExists();
        }

        return user;
    }

    @Transactional(readOnly = false)
    public void saveUser(User dataUser) {
        dataUser = userDao.find(dataUser.getId());
        userDao.save(dataUser);
    }

    @Transactional(readOnly = false)
    public void updateEmail(User dataUser, String email) {
        dataUser.setEmail(email);
        userDao.save(dataUser);
    }

    public void updatePassword(User dataUser, String originalPassword, String password) {
        if (!dataUser.getPasswordhash().equals(VideostreamerUtils.hashMd5(originalPassword))) {
            throw new UserInvalidPassword();
        }

        dataUser.setPasswordhash(VideostreamerUtils.hashMd5(password));
        userDao.save(dataUser);
    }
}
