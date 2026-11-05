package com.finanzapp.lifehub.modules.finance.movements;

import com.finanzapp.movements.repository.MovementRepository;
import com.finanzapp.movements.repository.mapper.MovementEntityMapper;
import com.finanzapp.movements.service.domain.Movement;
import com.finanzapp.movements.service.domain.MovementType;
import com.finanzapp.movements.service.domain.User;
import com.finanzapp.movements.service.impl.MovementServiceImpl;
import com.finanzapp.movements.service.impl.TagsClientImpl;
import com.finanzapp.movements.service.impl.UsersClientImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NetWorthCalculationTests {

    @Mock
    private MovementRepository movementRepository;

    @Mock
    private MovementEntityMapper movementMapper;

    @Mock
    private UsersClientImpl usersClient;

    @Mock
    private TagsClientImpl tagsClient;

    @Test
    void calculatesInitialWealthPlusBenefitsMinusExpenses() {
        String email = "user@example.com";
        List<Movement> movements = List.of(
                movement(MovementType.BENEFIT, "500.00"),
                movement(MovementType.ESSENTIAL_EXPENSE, "100.00"),
                movement(MovementType.DISCRETIONARY_EXPENSE, "50.00"),
                movement(MovementType.INVESTMENT, "200.00")
        );

        when(usersClient.getUser("token")).thenReturn(User.builder()
                .initialWealth(new BigDecimal("1000.00"))
                .build());
        when(movementRepository.findByUserEmail(email)).thenReturn(List.of());
        when(movementMapper.toDomainList(List.of())).thenReturn(movements);

        BigDecimal result = new MovementServiceImpl(
                movementRepository,
                movementMapper,
                usersClient,
                tagsClient
        ).getNetWorth(email, "token");

        assertThat(result).isEqualByComparingTo("1350.00");
    }

    private Movement movement(MovementType type, String amount) {
        return Movement.builder()
                .movementType(type)
                .amount(new BigDecimal(amount))
                .build();
    }
}
