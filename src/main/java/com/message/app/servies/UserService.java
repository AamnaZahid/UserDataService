package com.message.app.servies;

import com.message.app.configration.SecurityConfig;
import com.message.app.dto.UserDto;
import com.message.app.dto.UserLoginDto;
import com.message.app.model.User;
import com.message.app.repository.UserRepository;
import com.message.app.util.Constant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.session.*;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class UserService {
    @Autowired
    private SecurityConfig securityConfig;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FindByIndexNameSessionRepository<? extends Session> sessionRepository;

    @Autowired
    private ListOperations<String, Object> redisListOperations;

    @Autowired
    private RedisListService redisListService;
    private static final Logger logger = (Logger) LoggerFactory.getLogger(UserService.class);

    public String register(UserDto userDto) {
        String response = null;
        try {
            if (userRepository.existsByEmail(userDto.getEmail())) {
                response = Constant.ALREADY_EXIST;
            }
            User user = new User();

            String password = securityConfig.encode(userDto.getPassword());
            user.setUserName(userDto.getUserName());
            user.setEmail(userDto.getEmail());
            user.setPassword(password);
            userRepository.save(user);
            response = Constant.REGISTERED;
        } catch (Exception e) {
            logger.error("Error creating session: {}", e.getMessage());
            response = Constant.FAILED;
        }
        return response;
    }


    public String login(UserLoginDto loginDto) {
        User user = userRepository.findByEmail(loginDto.getEmail());
        String response = null;
        if (user != null && securityConfig.passwordEncoder().matches(loginDto.getPassword(), user.getPassword())) {
            try {
                Session session = sessionRepository.createSession();
                String sessionData = "User: " + loginDto.getEmail();
                redisListService.pushDataToList("user:sessions:" + session.getId(), sessionData);
                logger.info("Session created and data pushed to Redis for user: {}", loginDto.getEmail());
                logger.info("Session ID: " + session.getId());
                response = Constant.SUCCESS;
            } catch (Exception e) {
                logger.error("Error creating session: {}", e.getMessage());
                response = Constant.FAILED;
            }
        }
        return response;
    }

}



