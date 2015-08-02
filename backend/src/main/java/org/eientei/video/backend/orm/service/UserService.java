package org.eientei.video.backend.orm.service;

import com.googlecode.genericdao.search.Search;
import org.eientei.video.backend.orm.dao.UserDAO;
import org.eientei.video.backend.orm.entity.User;
import org.eientei.video.backend.orm.error.AlreadyExists;
import org.eientei.video.backend.orm.error.TooManyStreams;
import org.eientei.video.backend.orm.error.WrongPassword;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * User: iamtakingiteasy
 * Date: 2015-05-04
 * Time: 10:58
 */
@Service
@Transactional(readOnly = true)
public class UserService {
    @Autowired
    private UserDAO userDAO;

    @Autowired
    private StreamService streamService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public User getUserByUserName(String username) {
        Search search = new Search();
        search.addFilterCustom("lower(cast({name} as text)) = lower(cast(?1 as text))", username);
        return userDAO.searchUnique(search);
    }

    @Transactional(readOnly = false)
    public void createUser(String username, String password, String email, boolean createStream) throws AlreadyExists, TooManyStreams {
        if (getUserByUserName(username) != null || streamService.getStream("live", username) != null) {
            throw new AlreadyExists();
        }

        User user = new User();
        user.setName(username);
        user.setPasswordhash(password == null ? null : hashMd5(password));
        user.setEmail(email);
        userDAO.save(user);

        if (createStream) {
            streamService.allocateStream(user);
        }
    }

    @Transactional(readOnly = false)
    public void updateUser(User user) {
        userDAO.save(user);
    }

    @Transactional(readOnly = false)
    public void updateEmail(User user, String email) {
        userDAO.refresh(user);
        user.setEmail(email);
        userDAO.save(user);
    }

    @Transactional(readOnly = false)
    public void updatePassword(User user, String current, String desired) throws WrongPassword {
        userDAO.refresh(user);
        if (current != null && !user.getPasswordhash().equals(hashMd5(current))) {
            throw new WrongPassword();
        }
        user.setResetkey(null);
        user.setPasswordhash(hashMd5(desired));
        userDAO.save(user);
    }

    private String hashMd5(String data) {
        return passwordEncoder.encode(data);
    }

    public User getUserByEmail(String name, String email) {
        Search search = new Search();
        search.addFilterCustom("lower(cast({name} as text)) = lower(cast(?1 as text))", name);
        search.addFilterCustom("lower(cast({email} as text)) = lower(cast(?1 as text))", email);
        return userDAO.searchUnique(search);
    }

    @Transactional(readOnly = false)
    public String resetPassword(User user) {
        String hash = hashMd5(UUID.randomUUID().toString());
        userDAO.refresh(user);
        if (user.getResetkey() != null) {
            return null;
        }
        user.setResetkey(hash);
        userDAO.save(user);
        return hash;
    }

    public User getUserByResetKey(String resetKey) {
        Search search = new Search();
        search.addFilterEqual("resetkey", resetKey);
        return userDAO.searchUnique(search);
    }
}
