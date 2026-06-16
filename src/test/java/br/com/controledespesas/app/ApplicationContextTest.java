package br.com.controledespesas.app;

import br.com.controledespesas.service.AutenticacaoService;
import br.com.controledespesas.session.SessaoUsuario;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class ApplicationContextTest {

    @Test
    void shouldShareTheSameSessionInstanceWithAuthenticationService() throws Exception {
        ApplicationContext applicationContext = new ApplicationContext();

        SessaoUsuario sessaoUsuario = applicationContext.getSessaoUsuario();
        AutenticacaoService autenticacaoService = applicationContext.getAutenticacaoService();

        Field field = AutenticacaoService.class.getDeclaredField("sessaoUsuario");
        field.setAccessible(true);
        Object sessionInsideService = field.get(autenticacaoService);

        assertNotNull(applicationContext.getAsyncTaskExecutor());
        assertNotNull(applicationContext.getTransacaoService());
        assertNotNull(applicationContext.getDashboardService());
        assertNotNull(applicationContext.getCofrinhoService());
        assertNotNull(applicationContext.getMovimentacaoCofrinhoService());
        assertNotNull(applicationContext.getDashboardDAO());
        assertNotNull(applicationContext.getCofrinhoDAO());
        assertNotNull(applicationContext.getMovimentacaoCofrinhoDAO());
        assertSame(sessaoUsuario, sessionInsideService);
    }
}
