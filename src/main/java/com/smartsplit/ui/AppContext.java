package com.smartsplit.ui;

import com.smartsplit.dao.ExpenseDAO;
import com.smartsplit.dao.GroupDAO;
import com.smartsplit.dao.UserDAO;
import com.smartsplit.database.DBConnection;
import com.smartsplit.service.AuthenticationService;
import com.smartsplit.service.ExpenseService;
import com.smartsplit.service.GroupService;
import com.smartsplit.service.SettlementService;

/**
 * Single-instance holder for all services and DAOs.
 * Passed from frame to frame so nothing is re-instantiated.
 */
public class AppContext {

    public final DBConnection         db;
    public final UserDAO              userDao;
    public final GroupDAO             groupDao;
    public final ExpenseDAO           expenseDao;
    public final AuthenticationService auth;
    public final GroupService         groups;
    public final ExpenseService       expenses;
    public final SettlementService    settlements;

    public AppContext(DBConnection db) {
        this.db          = db;
        this.userDao     = new UserDAO(db);
        this.groupDao    = new GroupDAO(db);
        this.expenseDao  = new ExpenseDAO(db);
        this.auth        = new AuthenticationService(userDao);
        this.settlements = new SettlementService();
        this.groups      = new GroupService(groupDao, userDao, expenseDao, auth);
        this.expenses    = new ExpenseService(expenseDao, groupDao, auth, settlements);
    }
}
