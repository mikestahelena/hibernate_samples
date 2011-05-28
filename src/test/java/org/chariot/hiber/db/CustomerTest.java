package org.chariot.hiber.db;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;

import java.math.BigDecimal;

import static junit.framework.Assert.*;

@ContextConfiguration("classpath:META-INF/spring/*.xml")
@RunWith(SpringJUnit4ClassRunner.class)
@TransactionConfiguration(defaultRollback = false)
public class CustomerTest {

    private Logger logger = LoggerFactory.getLogger(CustomerTest.class);

    @Autowired
    private SessionFactory factory;

    @Autowired
    private PlatformTransactionManager manager;

    @Autowired
    protected void setTemplate(DataSource datasource) {
        template = new JdbcTemplate(datasource);
    }

    private JdbcTemplate template;

    /**
     * Create customers, within a separate transaction.  These customers
     * will be available for the length of the test cycle
     */
    @BeforeTransaction
    public void setupManyCustomers() {
        int results = template.update("delete from Customer");
        Session session = factory.openSession();
        for (int i = 0; i < 5; i++) {
            Customer c = new Customer();
            c.setFirstName("Customer #" + (i + 1));
            c.setLastName("Person");
            session.save(c);
        }
        session.flush();
        session.close();
    }

    @Test
    @Transactional
    public void introspectStatistics() {
        Statistics statistics = factory.getStatistics();
        logger.trace(statistics.toString());
    }

    @Transactional
    @Test
    @Rollback(true)
    public void addAndVerifyCustomer() {

        Session session = factory.getCurrentSession();
        Customer c = new Customer();
        c.setFirstName("Phil");
        c.setLastName("ADelphia");

        assertNull(c.getId());

        session.save(c);
        session.flush();
        session.clear();

        Customer c2 = (Customer)session.load(Customer.class, c.getId());

        assertNotNull(c2.getId());
        assertEquals(c.getId(), c2.getId());
        assertTrue( c != c2);

        /*session.flush(); */
    }

    @Test
    @Transactional
    public void testCached() {
        Session session = factory.getCurrentSession();
        Customer c = (Customer) session.createQuery("select c from Customer c " +
                "where c.id = (select min(c2.id) from Customer c2)").uniqueResult();
        assertNotNull(c);
        assertTrue(factory.getCache().containsEntity(Customer.class, c.getId()));
    }

    @Test
    @Transactional
    public void testNotCached() {
        Session session = factory.getCurrentSession();
        Product p = new Product();
        p.setListPrice(new BigDecimal("100.00"));
        p.setName("The Widget");
        session.save(p);
        session.flush();
        assertFalse(factory.getCache().containsEntity(Product.class, p.getId()));
    }

    @Test
    @Transactional
    public void testSize() {
        Session session = factory.getCurrentSession();
        Long size = (Long) session.createQuery("select count(c) from Customer c").uniqueResult();
        assertEquals(Long.valueOf(5), size);

    }

    @Test
    @Transactional
    public void createALotOfStuff() {
        
    }

}
