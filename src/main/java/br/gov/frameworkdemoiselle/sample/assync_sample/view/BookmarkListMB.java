package br.gov.frameworkdemoiselle.sample.assync_sample.view;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import br.gov.frameworkdemoiselle.annotation.NextView;
import br.gov.frameworkdemoiselle.annotation.PreviousView;
import br.gov.frameworkdemoiselle.sample.assync_sample.business.BookmarkBC;
import br.gov.frameworkdemoiselle.sample.assync_sample.domain.Bookmark;
import br.gov.frameworkdemoiselle.sample.assync_sample.util.TrabalhadorAssincrono;
import br.gov.frameworkdemoiselle.stereotype.ViewController;
import br.gov.frameworkdemoiselle.template.AbstractListPageBean;
import br.gov.frameworkdemoiselle.transaction.Transactional;

@ViewController
@NextView("./bookmark_edit.xhtml")
@PreviousView("./bookmark_list.xhtml")
public class BookmarkListMB extends AbstractListPageBean<Bookmark, Long> {

	private static final long serialVersionUID = 1L;
	
	private String textoLink;
	
	private ExecutorService eService;

	@Inject
	private BookmarkBC bc;
	
	public String getTextoLink() {
		return textoLink;
	}

	public void setTextoLink(String textoLink) {
		this.textoLink = textoLink;
	}

	@Override
	protected List<Bookmark> handleResultList() {
		return this.bc.findAll();
	}

	@Transactional
	public String deleteSelection() {
		boolean delete;
		for (Iterator<Long> iter = getSelection().keySet().iterator(); iter.hasNext();) {
			Long id = iter.next();
			delete = getSelection().get(id);

			if (delete) {
				bc.delete(id);
				iter.remove();
			}
		}
		return getPreviousView();
	}
	
	public void testAsync() {

		// Este runnable será nossa tarefa assíncrona
		TrabalhadorAssincrono trabalhador = new TrabalhadorAssincrono(this);

		if (eService == null) {
			eService = Executors.newCachedThreadPool();
		}

		eService.execute(trabalhador);
	}

}
