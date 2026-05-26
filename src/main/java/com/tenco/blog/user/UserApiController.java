package com.tenco.blog.user;

import com.tenco.blog._core.util.Define;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

// AJAX 통신 요청을 받아서 처리하는 컨트롤러
@RestController // Controller
@RequiredArgsConstructor
public class UserApiController {
    private final MailService mailService;
    private final UserService userService;;


    // 인증 번호 발송 시작
    // 주소설계: http:///localhost:8080/api/email/send POST
    // @RequestBody: 클라이언트 단에서 application/json 형식으로 요청을 준다라고 약속
    // 생성된 HTTP 요청 메세지에 요청 본문(Body)값을 추출해서 객체로 변관
    @PostMapping("/api/email/send")
    public ResponseEntity<?> sendMail(@RequestBody UserRequest.EmailCheckDTO reqDTO) {
        reqDTO.validate();
        mailService.인증번호발송(reqDTO.getEmail());
        return ResponseEntity.ok("인증 번호 발송됨");
    }

    /**
     * "email": 본인@naver.com
     * "code":123456
     *
     * @return
     */
    // 인증 번호 검증 요청 API
    @PostMapping("/api/email/verify")
    public ResponseEntity<?> 인증번호확인(@RequestBody UserRequest.EmailCheckDTO reqDTO) {
        reqDTO.validate();

        if (reqDTO.getCode() == null || reqDTO.getCode().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "인증번호를 입력해 주세요"));
        }

        // 인증 번호 검사 로직 메일 서비스로 위임
        boolean isVerified = mailService.인증번호확인(reqDTO.getEmail(), reqDTO.getCode());
        // 결과에 따른 응답처리
        if (isVerified) {
            return ResponseEntity.ok().body(Map.of("message", "인증되었습니다."));
        } else {
            return ResponseEntity.badRequest().body(Map.of("message", "인증번호가 일치하지 않습니다."));
        }
    }

    // 포인트 충전 API 설계 (JSON 형식으로 값이 들어옴)
    @PostMapping("/api/point/charge")
    public ResponseEntity<?> chargePoint(@RequestBody UserRequest.PointChargeDTO reqDTO,
                                         HttpSession session){

        // 1. 유효성 검사
        reqDTO.validate();

        // 2. 세션에서 사용자 정보 추출(로그인 된 상태만 접근 가능)
        User sessionUser = (User) session.getAttribute(Define.SESSION_USER);
        if(sessionUser == null) {
            return ResponseEntity.status(401).body(Map.of("message","로그인이 필요합니다"));
        }

        // 3. 포인트 충전 처리(비즈니스 로직)
        User updatedUser = userService.포인트충전(sessionUser.getId(),reqDTO.getAmount());
        // 4. 세션 동기화 처리(user.point값 갱신)
        session.setAttribute(Define.SESSION_USER,updatedUser);
        return ResponseEntity.ok()
                .body(Map.of("message","포인트가 충전 되었습니다.",
                        "point", updatedUser.getPoint()));
    }

}
