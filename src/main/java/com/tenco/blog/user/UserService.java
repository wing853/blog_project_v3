package com.tenco.blog.user;

import com.tenco.blog._core.errors.Exception400;
import com.tenco.blog._core.errors.Exception403;
import com.tenco.blog._core.errors.Exception404;
import com.tenco.blog._core.util.FileUtil;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

/**
 * User 관련 비즈니스 로직을 처리하는 Service 계층
 * Controller 와 Repository 사이에서 실제 업무 로직을 담당
 */
@Slf4j
@Service // IoC
@RequiredArgsConstructor // DI
@Transactional(readOnly = true) // 기본적인 읽기 전용 트랜잭션 처리 , 조회시 더티 체킹 안 일어남
public class UserService {

    private final UserRepository userRepository;
    // DI
    private final PasswordEncoder passwordEncoder;

    private final HttpSession session;

    // 초기 파미미터 값을 가져 오는 방법
    @Value("${oauth.kakao.client-id}")
    private String kakaoClientId;

    @Value("${oauth.kakao.client-secret}")
    private String kakaoClientSecret;

    @Value("${tenco.key}")
    private String tencoKey;



    // http://192.168.4.101:8080/join-form (강사 서버 컴퓨터 주소)

    /**
     * 회원 가입 처리
     *
     * @param joinDTO (사용자 회원가입 요청 정보)
     * @return User (저장된 사용자 정보)
     */
    @Transactional
    public User 회원가입(UserRequest.JoinDTO joinDTO) {
        log.info("회원가입 서비스 시작");
        // [핵심] 이메일 인증 도장 확인
        String verifiedEmail = (String) session.getAttribute("verified_email");

        if(verifiedEmail == null || !verifiedEmail.equals(joinDTO.getEmail())) {
            // 이메일 위변조를 방지하기위해 인증번호 검증시 넣었던 그 이메일을 진행시켜야 한다.
            throw new Exception400("이메일 인증을 완료해 주세요");
        }

        userRepository.findByUsername(joinDTO.getUsername()).ifPresent(
                user -> {
                    log.warn("회원가입 실패 - 중복된 사용자명: {} ",user.getUsername());
                    throw new Exception400("중복된 사용자 명입니다.");
                }
        );

        // 중복 이메일 체크
        // ifPresent -> 값이 존재하면 괄호안에 코드를 수행해
        userRepository.findByEmail(joinDTO.getEmail()).ifPresent(
                user -> {
                    log.warn("회원가입 실패 - 중복된 이메일: {} ",user.getEmail());
                    throw new Exception400("이미 존재하는 이메일입니다.");
                }
        );

        String profileImage = null;
        if (joinDTO.getProfileImage() != null && !joinDTO.getProfileImage().isEmpty()) {


            try {
                if (!FileUtil.isImageFile(joinDTO.getProfileImage())) {
                    throw new Exception400("이미지 파일만 업로드 가능합니다.");
                }

                profileImage = FileUtil.saveFile(joinDTO.getProfileImage(), FileUtil.IMAGES_DIR);
            } catch (IOException e) {
                throw new Exception400("프로필 업로드 실패");
            }

        }

        User user = joinDTO.toEntity(profileImage);
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // 이메일 인증 도장 삭제
        session.removeAttribute("verified_email");
        return userRepository.save(user);
    }

    /**
     * 로그인 처리
     *
     * @param loginDTO (사용자가 요청한 로그인 정보)
     * @return User(조회된 정보 세션 저장용)
     */
    public User 로그인(UserRequest.LoginDTO loginDTO) {

        User userEntity = userRepository.findByUsernameWithRoles(loginDTO.getUsername()).orElseThrow(
                () -> new Exception400("사용자 이름 또는 비밀번호를 잘못 입력하셨습니다.")
        );

        if (!passwordEncoder.matches(loginDTO.getPassword(), userEntity.getPassword())) {
            throw new Exception400("사용자 이름 또는 비밀번호를 잘못 입력하셨습니다.");
        }

        return userEntity;
    }

    /**
     * 사용자 정보 조회 (프로필 정보 보기 활용)
     *
     * @param id (User PK)
     * @return UserEntity
     */
    public User 회원정보수정화면(Integer id) {
        log.info("사용자 정보 서비스 시작");
        User userEntity = userRepository.findById(id).orElseThrow(() -> {
            log.warn("사용자 정보 조회 실패");
            return new Exception404("사용자 정보를 찾을 수 없습니다");
        });
        return userEntity;
    }


    /**
     * 사용자 정보 수정 처리 (프로필 업데이트)
     *
     * @param id        (User PK)
     * @param updateDTO (사용자가 요청한 데이터)
     * @return User
     */
    @Transactional
    public User 회원정보수정(Integer id, UserRequest.UpdateDTO updateDTO) {

        String newProfileImageFileName = null;

        // 1. 조회
        User userEntity = userRepository.findById(id).orElseThrow(
                () -> new Exception404("사용자를 찾을 수 없습니다.")
        );

        // 2. 인가처리 - 권한 확인
        if (!userEntity.getId().equals(id)) {
            throw new Exception403("회원 정보 수정 권한이 없습니다.");
        }

        // 3. 로직처리1 - 사용자가 비밀번호를 입력했을 경우 갱신
        if (updateDTO.getPassword() != null && !updateDTO.getPassword().isBlank()) {
            updateDTO.validate();
            String password = updateDTO.getPassword();
            updateDTO.setPassword(passwordEncoder.encode(password));
        } else {
            updateDTO.setPassword(null);
        }

        // 로직처리 2 - 사용자가 새로운 이미지를 등록했을경우
        if (updateDTO.getProfileImage() != null && !updateDTO.getProfileImage().isEmpty()) {

            try {
                if (!FileUtil.isImageFile(updateDTO.getProfileImage())) {
                    throw new Exception400("이미지 파일만 업로드 가능합니다.");
                }
                // 새 이미지 로컬 폴더에 저장(중복되지 않을 이미지 파일 이름을 리턴)
                newProfileImageFileName = FileUtil.saveFile(updateDTO.getProfileImage(), FileUtil.IMAGES_DIR);
                updateDTO.setProfileImageName(newProfileImageFileName);

                // 기존 이미지 파일 삭제(로컬에 계속 파일 쌓임)
                String oldProfileImageFilename = userEntity.getProfileImage();
                if (oldProfileImageFilename != null && !oldProfileImageFilename.isBlank()) {
                    FileUtil.deleteFile(oldProfileImageFilename, FileUtil.IMAGES_DIR);
                }

            } catch (IOException e) {
                throw new Exception400("파일 저장 실패");
            }

        }

        userEntity.update(updateDTO);

        return userEntity;
    }

    @Transactional
    public User 프로필이미지삭제(Integer id) {
        // 1. 정보 조회
        User userEntity = userRepository.findById(id).orElseThrow(
                () -> new Exception404("사용자를 찾을 수 없습니다")
        );
        // 2. 인가 처리
        if (userEntity.getId().equals(id) == false) {
            throw new Exception403("프로필 이미지 삭제 권한 없음");
        }

        // 3. 이미지가 등록되어 있으면 삭제 처리
        String profileImage = userEntity.getProfileImage();
        if (profileImage != null && !profileImage.isEmpty()) {
            // 내 서버 컴퓨터에 저장된(C://upload) 파일 삭제
            try {
                FileUtil.deleteFile(profileImage, FileUtil.IMAGES_DIR);
            } catch (IOException e) {
                System.err.println("프로필 이미지 삭제시 오류 발생 " + e.getMessage());
            }
        }
        // 1차 캐쉬에 저장된 User 정보 수정 - 트랜잭션이 종료 되면 반영(더티 체킹)
        userEntity.setProfileImage(null);
        return userEntity;
    }

    public User 사용자이름조회(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    // 1
    private UserResponse.OAuthToken 카카오엑세스토큰발급(String code) {
        RestTemplate restTemplate1 = new RestTemplate();

        HttpHeaders headers1 = new HttpHeaders();
        headers1.add("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");

        LinkedMultiValueMap<String, String> multiValueMap = new LinkedMultiValueMap();
        multiValueMap.add("grant_type", "authorization_code");
        multiValueMap.add("client_id", kakaoClientId);
        multiValueMap.add("redirect_uri", "http://localhost:8080/kakao-redirect");
        multiValueMap.add("code", code);
        multiValueMap.add("client_secret", kakaoClientSecret);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity(multiValueMap, headers1);

        ResponseEntity<UserResponse.OAuthToken> response1 = restTemplate1.exchange(
                "https://kauth.kakao.com/oauth/token",
                HttpMethod.POST,
                request,
                UserResponse.OAuthToken.class
        );
        UserResponse.OAuthToken oAuthToken = response1.getBody();

        return oAuthToken;
    }

    // 2
    private UserResponse.KakaoProfile 카카오프로필조회(String token) {
        String accessToken = token;
        RestTemplate restTemplate2 = new RestTemplate();

        HttpHeaders headers2 = new HttpHeaders();
        // 주의! 반드시 Bearer + "공백한칸" + 토큰
        headers2.add("Authorization", "Bearer " + accessToken);
        headers2.add("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");

        HttpEntity request2 = new HttpEntity(headers2);

        // HTTP 요청 2
        ResponseEntity<UserResponse.KakaoProfile> response2 = restTemplate2.exchange(
                "https://kapi.kakao.com/v2/user/me",
                HttpMethod.POST,
                request2,
                UserResponse.KakaoProfile.class
        );

        UserResponse.KakaoProfile kakaoProfile = response2.getBody();
        return kakaoProfile;
    }

    // 3

    private User 카카오조회및자동회원가입처리(UserResponse.KakaoProfile kakaoProfile) {
        // 고유한 username 생성(중복 방지용)
        String username = kakaoProfile.getKakaoAccount().getProfile()
                .getNickname() + "_" + kakaoProfile.getId();
        // 회원가입 여부 확인
        User user = 사용자이름조회(username);

        if(user == null) {
            log.info("기존 회원이 아님 자동 회원가입 진행");
            User newUser = User.builder()
                    .username(username)
                    .password(passwordEncoder.encode(tencoKey)) // 임시 비밀번호(노출 절대 금지)
                    .email(username+"kakao.com") // 임의 이메일 설정(추후 DB제약 방지)
                    .oAuthProvider(OAuthProvider.KAKAO) // 로그인 경로 설정
                    .build();

            String profileImage = kakaoProfile.getKakaoAccount().getProfile().getProfileImageUrl();
            if(profileImage != null && !profileImage.isEmpty()) {
                newUser.setProfileImage(profileImage);
            }
            user = userRepository.save(newUser);
        } else {
            System.out.println("이미 가입된 사용자라 바로 로그인 처리");
        }

        return user;
    }

    @Transactional
    public User 카카오소셜로그인(String code) {

        // 1. 발급 받은 인가 코드로 엑세스 토큰 발급 요청
        UserResponse.OAuthToken oAuthToken = 카카오엑세스토큰발급(code);
        // 2. 발급 받은 엑세스 토큰으로 사용자 카카오 프로필 조회
        UserResponse.KakaoProfile kakaoProfile = 카카오프로필조회(oAuthToken.getAccessToken());
        // 3. 응답 받은 결과로 우리 서버에 가입 여부 조회 및 자동 회원가입 처리
        User userEntity = 카카오조회및자동회원가입처리(kakaoProfile);
        // 4. 컨트롤러로 User 반환
        return userEntity;
    }


    @Transactional
    public User 포인트충전(Integer id, Integer amount) {
        // id값으로 db에 User 정보 조회
        User userEntity = userRepository.findById(id).orElseThrow(
                () -> new Exception404("사용자를 찾을 수 없습니다.")
        );
        userEntity.chargePoint(amount);

        return userRepository.save(userEntity);
    }
}




