package org.nutz.dao.impl.jdbc.oracle;

import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.nutz.dao.DB;
import org.nutz.dao.Dao;
import org.nutz.dao.Sqls;
import org.nutz.dao.entity.Entity;
import org.nutz.dao.entity.MappingField;
import org.nutz.dao.entity.PkType;
import org.nutz.dao.entity.annotation.ColType;
import org.nutz.dao.impl.jdbc.AbstractJdbcExpert;
import org.nutz.dao.impl.jdbc.BlobValueAdaptor2;
import org.nutz.dao.impl.jdbc.ClobValueAdapter2;
import org.nutz.dao.jdbc.JdbcExpertConfigFile;
import org.nutz.dao.jdbc.Jdbcs;
import org.nutz.dao.jdbc.ValueAdaptor;
import org.nutz.dao.pager.Pager;
import org.nutz.dao.sql.Pojo;
import org.nutz.dao.sql.Sql;
import org.nutz.dao.util.Pojos;
import org.nutz.lang.Mirror;

public class OracleJdbcExpert extends AbstractJdbcExpert {

    private static String CSEQ = "CREATE SEQUENCE ${T}_${F}_SEQ  MINVALUE 1"
                                 + " MAXVALUE 999999999999 INCREMENT BY 1 START"
                                 + " WITH 1 CACHE 20 NOORDER  NOCYCLE";
    private static String DSEQ = "DROP SEQUENCE ${T}_${F}_SEQ";

    private static String CTRI = "create or replace trigger ${T}_${F}_ST"
                                 + " BEFORE INSERT ON ${T}"
                                 + " FOR EACH ROW"
                                 + " BEGIN "
                                 + " IF :new.${F} IS NULL THEN"
                                 + " SELECT ${T}_${F}_seq.nextval into :new.${F} FROM dual;"
                                 + " END IF;"
                                 + " END ${T}_${F}_ST;";

    public OracleJdbcExpert(JdbcExpertConfigFile conf) {
        super(conf);
    }

    public ValueAdaptor getAdaptor(MappingField ef) {
        Mirror<?> mirror = ef.getTypeMirror();
        if (mirror.isBoolean())
            return new OracleBooleanAdaptor();
        if (mirror.isOf(Clob.class))
            return new ClobValueAdapter2(Jdbcs.getFilePool());
        if (mirror.isOf(Blob.class))
            return new BlobValueAdaptor2(Jdbcs.getFilePool());
        return super.getAdaptor(ef);
    }

    public boolean createEntity(Dao dao, Entity<?> en) {
        StringBuilder sb = new StringBuilder("CREATE TABLE " + en.getTableName() + "(");
        // ????????????
        for (MappingField mf : en.getMappingFields()) {
            if (mf.isReadonly())
                continue;
            sb.append('\n').append(mf.getColumnNameInSql());
            sb.append(' ').append(evalFieldType(mf));
            // ???????????? @Name??????????????????????????????
            if (mf.isName() && en.getPkType() != PkType.NAME) {
                sb.append(" NOT NULL UNIQUE");
            }
            // ????????????
            else {
                if (mf.isPk() && en.getPks().size() == 1)
                    sb.append(" primary key ");
                if (mf.isNotNull())
                    sb.append(" NOT NULL");
                if (mf.hasDefaultValue() && mf.getColumnType() != ColType.BOOLEAN)
                    addDefaultValue(sb, mf);
                if (mf.isUnsigned() && mf.getColumnType() != ColType.BOOLEAN) // ????????????
                    sb.append(" Check ( ").append(mf.getColumnNameInSql()).append(" >= 0)");
            }
            sb.append(',');
        }

        // ?????????????????????
        sb.setCharAt(sb.length() - 1, ')');

        List<Sql> sqls = new ArrayList<Sql>();
        sqls.add(Sqls.create(sb.toString()));

        // ??????????????????
        List<MappingField> pks = en.getPks();
        if (pks.size() > 1) {
            StringBuilder pkNames = new StringBuilder();
            for (MappingField pk : pks) {
                pkNames.append(pk.getColumnName()).append(',');
            }
            pkNames.setLength(pkNames.length() - 1);

            String pkNames2 = makePksName(en);

            String sql = String.format("alter table %s add constraint primary_key_%s primary key (%s)",
                                       en.getTableName(),
                                       pkNames2,
                                       pkNames);
            sqls.add(Sqls.create(sql));
        }
        // // ???????????????unique
        // for (MappingField mf : en.getMappingFields()) {
        // if(!mf.isPk())
        // continue;
        // String sql =
        // gSQL("alter table ${T} add constraint unique_key_${F} unique (${F});",
        // en.getTableName(),mf.getColumnName());
        // sqls.add(Sqls.create(sql));
        // }
        // ??????AutoIncreasement
        for (MappingField mf : en.getMappingFields()) {
            if (!mf.isAutoIncreasement())
                continue;
            // ??????
            sqls.add(Sqls.create(gSQL(CSEQ, en.getTableName(), mf.getColumnName())));
            // ?????????
            sqls.add(Sqls.create(gSQL(CTRI, en.getTableName(), mf.getColumnName())));
        }

        // ????????????
        sqls.addAll(createIndexs(en));

        // TODO ????????????Clob
        // TODO ????????????Blob

        // ??????????????????
        dao.execute(sqls.toArray(new Sql[sqls.size()]));
        // ???????????????
        createRelation(dao, en);
        // ????????????(????????????????????????)
        addComment(dao, en);

        return true;
    }

    public void formatQuery(Pojo pojo) {
        Pager pager = pojo.getContext().getPager();
        // ??????????????????
        if (null != pager && pager.getPageNumber() > 0) {
            pojo.insertFirst(Pojos.Items.wrap("SELECT * FROM (SELECT T.*, ROWNUM RN FROM ("));
            pojo.append(Pojos.Items.wrapf(") T WHERE ROWNUM <= %d) WHERE RN > %d",
                                          pager.getOffset() + pager.getPageSize(),
                                          pager.getOffset()));
        }
    }

    @Override
    public void formatQuery(Sql sql) {
        Pager pager = sql.getContext().getPager();
        // ??????????????????
        if (null != pager && pager.getPageNumber() > 0) {
            String pre = "SELECT * FROM (SELECT T.*, ROWNUM RN FROM (";
            String last = String.format(") T WHERE ROWNUM <= %d) WHERE RN > %d",
                                        pager.getOffset() + pager.getPageSize(),
                                        pager.getOffset());
            sql.setSourceSql(pre + sql.getSourceSql() + last);
        }
    }

    public String getDatabaseType() {
        return DB.ORACLE.name();
    }

    public String evalFieldType(MappingField mf) {
        if (mf.getCustomDbType() != null)
            return mf.getCustomDbType();
        switch (mf.getColumnType()) {
        case BOOLEAN:
            if (mf.hasDefaultValue())
                return "char(1) DEFAULT '"+getDefaultValue(mf)+"' check (" + mf.getColumnNameInSql() + " in(0,1))";
            return "char(1) check (" + mf.getColumnNameInSql() + " in(0,1))";
        case TEXT:
            return "CLOB";
        case VARCHAR:
            return "VARCHAR2(" + mf.getWidth() + ")";
        case INT:
            // ????????????????????????
            if (mf.getWidth() > 0)
                return "NUMBER(" + mf.getWidth() + ")";
            // ???????????????????????????
            return "NUMBER";

        case FLOAT:
            // ????????????????????????
            if (mf.getWidth() > 0 && mf.getPrecision() > 0) {
                return "NUMBER(" + mf.getWidth() + "," + mf.getPrecision() + ")";
            }
            // ???????????????
            if (mf.getTypeMirror().isDouble())
                return "NUMBER(15,10)";
            return "NUMBER";
        case TIME:
        case DATETIME:
        case DATE:
            return "DATE";
        default:
            return super.evalFieldType(mf);
        }
    }

    @Override
    protected String createResultSetMetaSql(Entity<?> en) {
        return "select * from " + en.getViewName() + " where rownum <= 1";
    }

    @Override
    public boolean dropEntity(Dao dao, Entity<?> en) {
        if (super.dropEntity(dao, en)) {
            if (en.getPks().isEmpty())
                return true;
            List<Sql> sqls = new ArrayList<Sql>();
            for (MappingField pk : en.getPks()) {
                if (pk.isAutoIncreasement()) {
                    String sql = gSQL(DSEQ, en.getTableName(), pk.getColumnName());
                    sqls.add(Sqls.create(sql));
                }
            }
            try {
                dao.execute(sqls.toArray(new Sql[sqls.size()]));
                return true;
            }
            catch (Exception e) {}
        }
        return false;
    }

    public boolean isSupportAutoIncrement() {
        return false;
    }
    
    public boolean addColumnNeedColumn() {
        return false;
    }
    
    public boolean supportTimestampDefault() {
        return false;
    }
    
    public String wrapKeywork(String columnName, boolean force) {
        if (force || keywords.contains(columnName.toUpperCase()))
            return "\"" + columnName + "\"";
        return null;
    }
    
    // https://docs.oracle.com/cd/B12037_01/server.101/b10755/statviews_1061.htm
    public List<String> getIndexNames(Entity<?> en, Connection conn) throws SQLException {
        List<String> names = new ArrayList<String>();
        String showIndexs = "SELECT * FROM user_indexes WHERE table_name='" + en.getTableName()+"'";
        PreparedStatement ppstat = conn.prepareStatement(showIndexs);
        ResultSet rest = ppstat.executeQuery();
        while (rest.next()) {
            String index = rest.getString(2);
            names.add(index);
        }
        return names;
    }
}
