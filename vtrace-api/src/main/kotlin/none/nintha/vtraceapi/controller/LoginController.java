package none.nintha.vtraceapi.controller;

import io.swagger.annotations.Api;
import none.nintha.vtraceapi.entity.Results;
import org.apache.logging.log4j.util.Strings;
import org.eclipse.collections.impl.factory.Maps;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.util.Objects;
@Api(tags = "Login")
@RestController
@RequestMapping("api/v1")
public class LoginController {
    @Autowired
    HttpSession session;
    @Value("@{default.username}")
    String defaultUsername;
    @Value("@{default.password}")
    String defaultPassword;

    @GetMapping("/login")
    public Results login(String username, String password) {
        if(Strings.isBlank(username) || Strings.isBlank(password)){
            return Results.failed("error username or password");
        }

        if (Objects.equals(username, defaultUsername) && Objects.equals(password, defaultPassword)) {
            logout();
            session.setAttribute("user", username);
            session.setMaxInactiveInterval(3600 * 24 * 7); // 过期时间1周
            return Results.success(null);
        }
        return Results.failed("error username or password");
    }

    @GetMapping("/logout")
    public Results logout() {
        session.removeAttribute("user");
        session.invalidate();
        return Results.success(null);
    }

    @GetMapping("/userInfo")
    public Results userInfo() {
        String user = (String) session.getAttribute("user");
        return Results.success(Maps.mutable.of("user", user));
    }

    @GetMapping("/401")
    public Results unlogin(){
        return Results.unlogin();
    }

}
