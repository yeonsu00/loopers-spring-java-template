package com.loopers.domain.user;

import com.loopers.application.user.UserCommand;
import com.loopers.domain.order.Order;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public User signup(UserCommand.SignupCommand signupCommand) {
        if (userRepository.existsByLoginId(signupCommand.loginId())) {
            throw new CoreException(ErrorType.CONFLICT, "이미 가입된 ID입니다.");
        }

        if (userRepository.existsByEmail(signupCommand.email())) {
            throw new CoreException(ErrorType.CONFLICT, "이미 가입된 이메일입니다.");
        }

        User user = User.createUser(signupCommand);
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public Optional<User> findUserByLoginId(String loginId) {
        return userRepository.findByLoginId(loginId);
    }

    public List<Order> findOrdersByUserId(Long userId) {
        List<Order> orders = userRepository.findOrdersByUserId(userId);

        if (orders.isEmpty()) {
            throw new CoreException(ErrorType.NOT_FOUND, "해당 사용자의 주문 내역이 없습니다.");
        }

        return orders;
    }
}
