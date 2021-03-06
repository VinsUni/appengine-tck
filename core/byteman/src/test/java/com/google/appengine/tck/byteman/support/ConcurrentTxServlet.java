/*
 * Copyright 2014 Google Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.appengine.tck.byteman.support;

import java.io.IOException;
import java.util.Random;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.api.datastore.TransactionOptions;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class ConcurrentTxServlet extends HttpServlet {
    private final Logger log = Logger.getLogger(getClass().getName());

    private final Random RANDOM = new Random();

    private final Entity ROOT_1 = new Entity("ROOT", 1);
    private final Entity ROOT_2 = new Entity("ROOT", 2);

    @Override
    public void init() throws ServletException {
        super.init();

        DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
        ds.put(ROOT_1); // create root 1
        ds.put(ROOT_2); // create root 2
    }

    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String entityGroup = req.getParameter("eg");
        String counter = req.getParameter("c");
        String parent = req.getParameter("p");
        boolean xg = Boolean.parseBoolean(req.getParameter("xg"));

        Key parentKey = "2".equals(parent) ? ROOT_2.getKey() : ROOT_1.getKey();

        Entity entity = new Entity(entityGroup, parentKey);
        entity.setProperty("foo", RANDOM.nextInt());

        DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
        final Transaction tx = ds.beginTransaction(TransactionOptions.Builder.withXG(xg));
        try {
            log.warning("Before put ... " + counter);
            putEntity(ds, entity);
            log.warning("After put ... " + counter);
            tx.commit();
            resp.getWriter().write("OK" + counter);
        } catch (Exception e) {
            log.warning("Error ... " + e);
            tx.rollback();
            resp.getWriter().write("ERROR" + counter + ":" + e.getClass().getName());
            error(counter);
        } finally {
            cleanup(counter);
        }
    }

    private void putEntity(DatastoreService ds, Entity entity) throws IOException {
        log.warning("Key = " + ds.put(entity));
    }

    private void error(String counter) {
        log.warning("Error = " + counter);
    }

    private void cleanup(String counter) {
        log.warning("Cleanup = " + counter);
    }
}
