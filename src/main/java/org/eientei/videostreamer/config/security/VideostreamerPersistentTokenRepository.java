package org.eientei.videostreamer.config.security;

import org.eientei.videostreamer.orm.entity.PersistentToken;
import org.eientei.videostreamer.orm.service.PersistentTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.web.authentication.rememberme.PersistentRememberMeToken;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * User: iamtakingiteasy
 * Date: 2015-05-03
 * Time: 14:54
 */
@Component
public class VideostreamerPersistentTokenRepository implements PersistentTokenRepository {
    @Autowired
    private PersistentTokenService persistentTokenService;

    @Override
    public void createNewToken(PersistentRememberMeToken token) {
        persistentTokenService.create(token.getUsername(), token.getSeries(), token.getTokenValue(), token.getDate());
    }

    @Override
    public void updateToken(String series, String tokenValue, Date lastUsed) {
        persistentTokenService.update(series, tokenValue, lastUsed);
    }

    @Override
    public PersistentRememberMeToken getTokenForSeries(String seriesId) {
        PersistentToken token = persistentTokenService.find(seriesId);
        if (token == null) {
            return null;
        }
        return new PersistentRememberMeToken(token.getUsername(), token.getSeries(), token.getToken(), token.getTimestamp());
    }

    @Override
    public void removeUserTokens(String username) {
        persistentTokenService.deleteByUserName(username);
    }
}
