package org.eientei.video.orm.service;

import com.googlecode.genericdao.search.Search;
import org.eientei.video.orm.dao.GroupDAO;
import org.eientei.video.orm.dao.UserDAO;
import org.eientei.video.orm.entity.Group;
import org.eientei.video.orm.entity.Role;
import org.eientei.video.orm.entity.User;
import org.eientei.video.orm.util.VideostreamUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * User: iamtakingiteasy
 * Date: 2014-09-25
 * Time: 22:04
 */
@Service
@Transactional(readOnly = true)
public class UserService {
    public static class UserAlreadyExists extends Exception { }

    @Autowired
    private UserDAO userDao;

    @Autowired
    private GroupDAO groupDao;

    public User getUserByName(String name) {
        Search search = new Search();
        search.addFilterEqual("name", name);
        User user = userDao.searchUnique(search);
        if (user != null) {
            for (Group group : user.getGroups()) {
                for (Role role : group.getRoles()) {
                }
            }
        }
        return user;
    }

    public User getUserByNameOrAnonymous(String name) {
        User user = getUserByName(name);
        if (user == null) {
            user = getUserByName("Anonymous");
        }
        return user;
    }

    public Group getGroupByName(String name) {
        Search search = new Search();
        search.addFilterEqual("name", name);
        Group group = groupDao.searchUnique(search);
        if (group != null) {
            for (Role role : group.getRoles()) {

            }
        }
        return group;
    }

    @Transactional(readOnly = false)
    public Group createGroup(String name) {
        Group group = new Group();
        group.setName(name);
        groupDao.save(group);
        return group;
    }

    @Transactional(readOnly = false)
    public User createUser(String username, String password, String email) throws UserAlreadyExists {
        if (getUserByName(username) != null) {
            throw new UserAlreadyExists();
        }

        User user = new User();
        user.setName(username);
        user.setPasswordhash(VideostreamUtils.hashMd5(password));
        user.setEmail(email);
        Group group = getGroupByName("User");
        if (group == null) {
            group = createGroup("User");
        }
        user.getGroups().add(group);
        try {
            userDao.save(user);
        } catch (Throwable t) {
            throw new UserAlreadyExists();
        }

        return user;
    }

    @Transactional(readOnly = false)
    public void saveUser(User dataUser) {
        userDao.save(dataUser);
    }
}
