package org.exoplatform.services.injection;

public interface DataInjector {
    void inject() throws Exception;
    void purge() throws Exception;
}
