package br.gov.frameworkdemoiselle.sample.assync_sample.util;

import java.util.List;
import java.util.TreeMap;

import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.spi.Context;
import javax.enterprise.inject.spi.BeanManager;
import javax.persistence.EntityManager;

import org.jboss.weld.context.RequestContext;
import org.jboss.weld.context.bound.BoundRequestContext;

import br.gov.frameworkdemoiselle.sample.assync_sample.domain.Bookmark;
import br.gov.frameworkdemoiselle.sample.assync_sample.view.BookmarkListMB;
import br.gov.frameworkdemoiselle.util.Beans;

/**
 * Trabalhador que cria um contexto temporário para controlar beans RequestScoped
 * caso ele seja invocado fora de um http request.
 * 
 * 
 * @author serpro
 *
 */
public class TrabalhadorAssincrono implements Runnable {
	
	private BookmarkListMB callback;

	private Context temporaryContext = null;

	public TrabalhadorAssincrono(BookmarkListMB callback){
		this.callback = callback;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void run() {

		// Dorme para simular uma tarefa longa
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		try {

			// Ativamos um contexto temporário se for necessário
			iniciarContexto();

			/*
			 * Demoiselle controla EntityManagers usando um gerenciador que é RequestScoped. Por conta disso é
			 * necessário haver contexto ativo para esse escopo. Se não houver contexto ativo, o erro dispara aqui.
			 */
			EntityManager em = Beans.getReference(EntityManager.class);

			List<Bookmark> bookmarks = em.createQuery("SELECT b FROM Bookmark b").getResultList();

			/*
			 * Se chegar aqui, o contexto existe e o processo segue normalmente.
			 */
			if (bookmarks != null && bookmarks.size() > 0) {
				callback.setTextoLink( bookmarks.get(0).getLink() );
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			// Desativamos o contexto temporário se ativamos um previamente
			terminarContexto();
		}
	}

	/*
	 * Esse método usa um contexto do próprio WELD (o BoundRequestContext) que é usado
	 * para simular requests não associados a uma Servlet.
	 * 
	 * Documentação:
	 *    http://docs.jboss.org/weld/reference/latest/en-US/html/contexts.html#contexts.available
	 *    http://docs.jboss.org/weld/javadoc/2.0/weld-api/org/jboss/weld/context/bound/BoundRequestContext.html 
	 */
	private void iniciarContexto() {
		BeanManager bm = Beans.getReference(BeanManager.class);

		try {
			// Testa se já existe contexto ativo.
			bm.getContext(RequestScoped.class);
		} catch (ContextNotActiveException ce) {
			// Não existe contexto ativo. Instanciamos um contexto que o WELD já tem pronto nesse caso.
			temporaryContext = Beans.getReference(BoundRequestContext.class);
			((BoundRequestContext) temporaryContext).associate(new TreeMap<String, Object>());
			((BoundRequestContext) temporaryContext).activate();
		}

	}

	/*
	 * Esse método testa se foi necessário criar um contexto temporário e o finaliza em caso positivo. 
	 * 
	 * Documentação:
	 *    http://docs.jboss.org/weld/reference/latest/en-US/html/contexts.html#contexts.available
	 *    http://docs.jboss.org/weld/javadoc/2.0/weld-api/org/jboss/weld/context/bound/BoundRequestContext.html 
	 */
	private void terminarContexto() {
		BeanManager bm = Beans.getReference(BeanManager.class);
		Context c = null;

		try {
			// Testa se já existe contexto ativo.
			c = bm.getContext(RequestScoped.class);

			// Se o contexto ativo é nosso contexto temporário, podemos desativa-lo.
			// Do contrário estamos em um contexto criado externamente, não fazemos nada.
			if (c == temporaryContext) {
				((RequestContext) temporaryContext).invalidate();
				((RequestContext) temporaryContext).deactivate();
			}
		} catch (ContextNotActiveException ce) {
			// Não existe contexto ativo, nada a terminar
		} finally {
			temporaryContext = null;
		}
	}

}
