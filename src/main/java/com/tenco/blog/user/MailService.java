package com.tenco.blog.user;

import com.tenco.blog._core.util.MailUtil;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * 매일 발송 서비스
 * - 비즈니스 로직
 * 1. 이메일 번호 생성
 * 2. 이메일 발송
 * 3. 세션에 인증 번호 저장(검증용)
 */

@Service
@RequiredArgsConstructor
public class MailService {
    // 스프링 부트가 제공하는 메일 발송 객체
    // application-dev.yml에 저장된 STMP 정보를 사용하여 메일을 전송
    private final JavaMailSender javaMailSender;
    private final HttpSession session;

    public void 인증번호발송(String email) {
        // 1. 인증 번호 생성
        String code = MailUtil.generateRandomCode();
        System.out.println("생성된 인증 번호: " + code);

        // 2. 이메일 발송(전송 내용 만들기)
        // MimeMessage란 이메일을 보낼때 내용을 작성하는 편지봉투
        // 텍스트 뿐만 아니라 HTML, 첨부파일등을 포함할 수 있는 포맷
        MimeMessage emailMessage = javaMailSender.createMimeMessage();

        try {
            //true: 멀티 파트 허용(파일 사용도 허용)
            MimeMessageHelper helper = new MimeMessageHelper(emailMessage, true, "utf-8");
            helper.setTo(email); // 받는 사람 설정
            helper.setSubject("[m-blog 회원 가입 인증 번호]"); // 메일에 제목 설정
            helper.setText("<h3>인증 번호는 " + code + "입니다.</h3>", true);
            javaMailSender.send(emailMessage);

            // 세션에 인증버호 임시 저장
            session.setAttribute("code_" + email, code);
            System.out.println("인증번호 발송 완료:" + code);// todo - 추후삭제
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean 인증번호확인(String email, String code) {
        // 1. 인증번호 발송 API --> 세션에 임시 코드값이 저장된 상태이다.
        String savedCode = (String) session.getAttribute("code_" + email); // 이 값으로 - value: 값을 추출
        if (savedCode != null && savedCode.equals(code)) {
            // 인증된 상태를 의미
            session.removeAttribute("code_" + email);

            // 이 이메일은 인증 완료됨이란 상태를 다시 세션에 저장(도장을 직어준다)
            // 회원 가입시 UserService에서 이 도장을 확인하고 회원 가입 처리를 한다
            session.setAttribute("verified_email", email);

            return true;
        } else {
            return false;
        }
    }
}
