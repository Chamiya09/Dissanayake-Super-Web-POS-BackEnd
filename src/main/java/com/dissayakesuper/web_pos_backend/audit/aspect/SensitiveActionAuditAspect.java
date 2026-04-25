package com.dissayakesuper.web_pos_backend.audit.aspect;

import com.dissayakesuper.web_pos_backend.audit.service.AuditLogService;
import com.dissayakesuper.web_pos_backend.user.entity.User;
import com.dissayakesuper.web_pos_backend.user.repository.UserRepository;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

@Aspect
@Component
public class SensitiveActionAuditAspect {

    private final AuditLogService auditLogService;
    private final UserRepository userRepository;

    public SensitiveActionAuditAspect(AuditLogService auditLogService,
                                      UserRepository userRepository) {
        this.auditLogService = auditLogService;
        this.userRepository = userRepository;
    }

    @Pointcut("execution(* com.dissayakesuper.web_pos_backend..service..*.delete*(..))")
    void deleteActions() {
    }

    @Pointcut("execution(* com.dissayakesuper.web_pos_backend.user.service.UserService.update(..))")
    void userUpdateAction() {
    }

    @Pointcut("execution(* com.dissayakesuper.web_pos_backend.sale.service.SaleService.updateSaleStatus(..)) && args(saleId,newStatus)")
    void saleStatusAction(Long saleId, String newStatus) {
    }

    @AfterReturning("deleteActions()")
    public void onDeleteAction(JoinPoint joinPoint) {
        Long actorUserId = resolveActorUserId();
        String detail = buildDetail(joinPoint);
        auditLogService.logAction(actorUserId, "DELETE", detail);
    }

    @AfterReturning("userUpdateAction()")
    public void onUserUpdate(JoinPoint joinPoint) {
        Long actorUserId = resolveActorUserId();
        String detail = buildDetail(joinPoint);
        auditLogService.logAction(actorUserId, "USER_UPDATE", detail);
    }

    @AfterReturning(pointcut = "saleStatusAction(saleId,newStatus)", argNames = "joinPoint,saleId,newStatus")
    public void onSaleStatusUpdate(JoinPoint joinPoint, Long saleId, String newStatus) {
        if (newStatus == null || !"Voided".equalsIgnoreCase(newStatus.trim())) {
            return;
        }

        Long actorUserId = resolveActorUserId();
        String detail = "saleId=" + saleId + ", method=" + joinPoint.getSignature().toShortString();
        auditLogService.logAction(actorUserId, "VOID", detail);
    }

    private Long resolveActorUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            return null;
        }

        User user = userRepository.findByUsername(auth.getName()).orElse(null);
        return user != null ? user.getId() : null;
    }

    private String buildDetail(JoinPoint joinPoint) {
        String argsString = Arrays.stream(joinPoint.getArgs())
                .map(arg -> arg == null ? "null" : String.valueOf(arg))
                .collect(Collectors.joining(", "));

        return "method=" + joinPoint.getSignature().toShortString() + ", args=[" + argsString + "]";
    }
}
