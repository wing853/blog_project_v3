package com.tenco.blog.user;

import com.tenco.blog._core.errors.Exception401;
import com.tenco.blog._core.util.Define;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Map;


@Slf4j
@Controller // IoC
@RequiredArgsConstructor // DI 처리
public class UserController {

    private final UserService userService;

    // 동의 항목 승인 이후 카카오 인가 서버에서 인가 코드가 리다이렉트
    @GetMapping("/kakao-redirect")
    public String kakaoCallback(@RequestParam(name = "code") String code, HttpSession session) {

        // 외부 통신을 할 때 예외 처리 권장
        try {
            User seeeionuser = userService.카카오소셜로그인(code);
            //  우리 서버 세션에 회원 정보 저장
            session.setAttribute(Define.SESSION_USER,seeeionuser);
        } catch (Exception e) {
            log.error("카카오 로그인 실패 " + e.getMessage());
            throw new Exception401("소셜 로그인 실패");
        }

        return "redirect:/";

    }

    // 프로필 이미지 삭제 요청
    @PostMapping("/user/profile-image/delete")
    public String deleteProfileImage(HttpSession session) {

        User sessionUser = (User) session.getAttribute(Define.SESSION_USER);
        // 프로필 이미지 삭제
        User updateUser = userService.프로필이미지삭제(sessionUser.getId());
        // 세션에 저장되어 있던 프로필이미지 삭제 후 세션 동기화 처리
        session.setAttribute(Define.SESSION_USER, updateUser);
        return "redirect:/user/detail";
    }


    // 마이페이지 요청 화면
    @GetMapping("/user/detail")
    public String detailPage(Model model, HttpSession session) {

        User sessionUser = (User) session.getAttribute(Define.SESSION_USER);
        model.addAttribute("user", sessionUser);
        return "user/detail";
    }


    // 회원 정보 수정 기능 요청
    @PostMapping("/user/update")
    public String updateProc(UserRequest.UpdateDTO updateDTO, HttpSession session) {
        // 회원 정보 수정 요청시 기본 비밀번호 null 이고 프로필 이미지만 수정 요청
        User sessionUser = (User) session.getAttribute(Define.SESSION_USER);
        // 프로필 이미지 변경 요청이 왔을 때 기존에 비밀번호 저장


        User updateUser = userService.회원정보수정(sessionUser.getId(), updateDTO);

        session.setAttribute(Define.SESSION_USER, updateUser);
        return "redirect:/";
    }

    // 프로필 화면 요청
    @GetMapping("/user/update-form")
    public String updateFormPage(HttpSession session, Model model) {
        User sessionUser = (User) session.getAttribute("sessionUser");
        User user = userService.회원정보수정화면(sessionUser.getId());
        model.addAttribute("user", user);
        return "user/update-form";
    }

    // 로그인 화면 요청
    // 주소 설계 - http://localhost:8080/login-form
    @GetMapping("/login-form")
    public String loginFormPage() {
        return "user/login-form";
    }

    // 로그인 기능 요청
    @PostMapping("/login")
    public String loginProc(UserRequest.LoginDTO reqLoginDTO, HttpSession session) {
        // 인증 검사 x, 유효성 검사 o
        reqLoginDTO.validate();
        User user = userService.로그인(reqLoginDTO);
        session.setAttribute("sessionUser", user);
        return "redirect:/";
    }


    // 로그아웃 기능 요청
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        // 세션 메모리에 내 정보를 없애 버림
        session.invalidate();
        return "redirect:/";
    }

    // 회원 가입 화면 요청
    // 주소 설계 - http://localhost:8080/join-form
    @GetMapping("/join-form")
    public String joinFormPage() {
        return "user/join-form";
    }

    // 회원 가입 기능 요청
    // 주소 설계 - http://localhost:8080/join
    @PostMapping("/join")
    public String joinProc(UserRequest.JoinDTO joinDTO) throws IOException {
        //  인증검사 x, 유효성 검사 하기 o
        joinDTO.validate();
        userService.회원가입(joinDTO);
        return "redirect:/login-form";
    }

}
