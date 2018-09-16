package none.nintha.vtraceapi.config;

import none.nintha.vtraceapi.entity.exceptions.UnloginException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * 自定义拦截器1
 */
public class VtracerInterceptor implements HandlerInterceptor {
    private final static Logger logger = LoggerFactory.getLogger(VtracerInterceptor.class);
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
//        System.out.println(">>>VtracerInterceptor>>>>>>>在请求处理之前进行调用（Controller方法调用之前）");
        String method = request.getMethod();
        // 对get方法不做登录校验
        if(method.toLowerCase().equals("get")){
            return true;
        }
        HttpSession session = request.getSession();
        Object user = session.getAttribute("user");
        if(user == null){
            logger.error("no login error, url=" + request.getRequestURI());
            throw new UnloginException();
        }
        return true;// 只有返回true才会继续向下执行，返回false取消当前请求
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
                           ModelAndView modelAndView) throws Exception {
//        System.out.println(">>>VtracerInterceptor>>>>>>>请求处理之后进行调用，但是在视图被渲染之前（Controller方法调用之后）");
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
            throws Exception {
//        System.out.println(">>>VtracerInterceptor>>>>>>>在整个请求结束之后被调用，也就是在DispatcherServlet 渲染了对应的视图之后执行（主要是用于进行资源清理工作）");
    }

}