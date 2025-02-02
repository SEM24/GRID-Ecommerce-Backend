package com.khomsi.backend.main.user.service;

import com.khomsi.backend.main.user.model.dto.BalanceUserInfoDTO;
import com.khomsi.backend.main.game.model.entity.Game;
import com.khomsi.backend.main.user.model.dto.FullUserInfoDTO;
import com.khomsi.backend.main.user.model.entity.UserInfo;
import org.springframework.security.oauth2.jwt.Jwt;

public interface UserInfoService {
    FullUserInfoDTO getCurrentUser();

    //Get credential of auth user through keycloak
    Jwt getJwt();

    BalanceUserInfoDTO getUserBalance();

    void checkPermissionToAction(String userId);

    UserInfo getExistingUser(String userInfo);

    boolean checkIfGameIsOwnedByCurrentUser(Game game);

    UserInfo getUserInfo();
}
