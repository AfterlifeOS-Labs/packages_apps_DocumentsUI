/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.documentsui.dirlist;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.ResolveInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import androidx.test.InstrumentationRegistry;

import com.android.documentsui.ActionHandler;
import com.android.documentsui.BaseActivity;
import com.android.documentsui.R;
import com.android.documentsui.TestConfigStore;
import com.android.documentsui.TestUserIdManager;
import com.android.documentsui.TestUserManagerState;
import com.android.documentsui.base.State;
import com.android.documentsui.base.UserId;
import com.android.documentsui.sidebar.AppItem;
import com.android.documentsui.sidebar.Item;
import com.android.documentsui.sidebar.RootItem;
import com.android.documentsui.testing.TestActionHandler;
import com.android.documentsui.testing.TestProvidersAccess;
import com.android.documentsui.testing.TestResolveInfo;
import com.android.documentsui.util.VersionUtils;
import com.android.modules.utils.build.SdkLevel;

import com.google.common.collect.Lists;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.util.ArrayList;
import java.util.List;

@RunWith(Parameterized.class)
public class AppsRowManagerTest {

    private AppsRowManager mAppsRowManager;

    private ActionHandler mActionHandler;
    private boolean mMaybeShowBadge;
    private BaseActivity mActivity;
    private TestUserIdManager mTestUserIdManager;
    private TestUserManagerState mTestUserManagerState;
    private State mState;

    private View mAppsRow;
    private LinearLayout mAppsGroup;
    private final TestConfigStore mTestConfigStore = new TestConfigStore();

    @Parameter(0)
    public boolean isPrivateSpaceEnabled;

    /**
     * Parametrize values for {@code isPrivateSpaceEnabled} to run all the tests twice once with
     * private space flag enabled and once with it disabled.
     */
    @Parameters(name = "privateSpaceEnabled={0}")
    public static Iterable<?> data() {
        return com.google.android.collect.Lists.newArrayList(true, false);
    }

    @Before
    public void setUp() {
        mActionHandler = new TestActionHandler();
        mTestUserIdManager = new TestUserIdManager();
        mTestUserManagerState = new TestUserManagerState();

        mAppsRowManager = getAppsRowManager();

        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        mState = new State();
        mActivity = mock(BaseActivity.class);
        mAppsRow = layoutInflater.inflate(R.layout.apps_row, null);
        mAppsGroup = mAppsRow.findViewById(R.id.apps_row);

        mState.configStore = mTestConfigStore;

        when(mActivity.getLayoutInflater()).thenReturn(layoutInflater);
        when(mActivity.getDisplayState()).thenReturn(mState);
        when(mActivity.findViewById(R.id.apps_row)).thenReturn(mAppsRow);
        when(mActivity.findViewById(R.id.apps_group)).thenReturn(mAppsGroup);
        when(mActivity.getSelectedUser()).thenReturn(TestProvidersAccess.USER_ID);

        isPrivateSpaceEnabled &= SdkLevel.isAtLeastS();
        if (isPrivateSpaceEnabled) {
            mTestConfigStore.enablePrivateSpaceInPhotoPicker();
        }
    }

    private AppsRowManager getAppsRowManager() {
        if (isPrivateSpaceEnabled && SdkLevel.isAtLeastS()) {
            mTestUserManagerState = new TestUserManagerState();
            mTestUserManagerState.userIds =
                    Lists.newArrayList(UserId.DEFAULT_USER, TestProvidersAccess.OtherUser.USER_ID,
                            TestProvidersAccess.AnotherUser.USER_ID);
            return new AppsRowManager(mActionHandler, mMaybeShowBadge, mTestUserManagerState,
                    mTestConfigStore);
        }
        mTestUserIdManager = new TestUserIdManager();
        mTestUserIdManager.userIds =
                Lists.newArrayList(UserId.DEFAULT_USER, TestProvidersAccess.OtherUser.USER_ID);
        return new AppsRowManager(mActionHandler, mMaybeShowBadge, mTestUserIdManager,
                mTestConfigStore);
    }

    @Test
    public void testUpdateList_byRootItem() {
        mActivity.setConfigStore(mTestConfigStore);
        final List<Item> rootList = new ArrayList<>();
        rootList.add(new RootItem(TestProvidersAccess.INSPECTOR, mActionHandler, mMaybeShowBadge));
        rootList.add(new RootItem(TestProvidersAccess.PICKLES, mActionHandler, mMaybeShowBadge));
        rootList.add(new RootItem(TestProvidersAccess.PICKLES, mActionHandler, "packageName",
                mMaybeShowBadge));
        rootList.add(new RootItem(TestProvidersAccess.PICKLES, mActionHandler, "packageName",
                mMaybeShowBadge));

        final List<AppsRowItemData> chipDataList = mAppsRowManager.updateList(rootList);

        assertEquals(chipDataList.size(), rootList.size());
        assertEquals(TestProvidersAccess.INSPECTOR.title, chipDataList.get(0).getTitle());
        assertNull(chipDataList.get(0).getSummary());
        assertEquals(TestProvidersAccess.PICKLES.title, chipDataList.get(1).getTitle());
        assertNull(chipDataList.get(1).getSummary());
        assertEquals(TestProvidersAccess.PICKLES.summary, chipDataList.get(2).getSummary());
        assertEquals(TestProvidersAccess.PICKLES.summary, chipDataList.get(3).getSummary());
    }

    @Test
    public void testUpdateList_byHybridItem() {
        mActivity.setConfigStore(mTestConfigStore);
        final String testPackageName = "com.test1";
        final ResolveInfo info = TestResolveInfo.create();
        info.activityInfo.packageName = testPackageName;

        List<Item> hybridList = new ArrayList<>();
        hybridList.add(
                new RootItem(TestProvidersAccess.INSPECTOR, mActionHandler, mMaybeShowBadge));
        hybridList.add(new AppItem(info, TestProvidersAccess.PICKLES.title, UserId.DEFAULT_USER,
                mActionHandler));

        final List<AppsRowItemData> chipDataList = mAppsRowManager.updateList(hybridList);

        assertEquals(chipDataList.size(), hybridList.size());
        assertEquals(TestProvidersAccess.INSPECTOR.title, chipDataList.get(0).getTitle());
        assertTrue(chipDataList.get(0) instanceof AppsRowItemData.RootData);
        assertEquals(TestProvidersAccess.PICKLES.title, chipDataList.get(1).getTitle());
        assertNull(chipDataList.get(1).getSummary());
        assertTrue(chipDataList.get(1) instanceof AppsRowItemData.AppData);
    }

    @Test
    public void testUpdateView_matchedState_showRow() {
        mActivity.setConfigStore(mTestConfigStore);
        mState.action = State.ACTION_BROWSE;
        mState.stack.changeRoot(TestProvidersAccess.RECENTS);
        final List<Item> rootList = new ArrayList<>();
        rootList.add(new RootItem(TestProvidersAccess.INSPECTOR, mActionHandler, mMaybeShowBadge));
        mAppsRowManager.updateList(rootList);

        mAppsRowManager.updateView(mActivity);

        assertEquals(View.VISIBLE, mAppsRow.getVisibility());
        assertEquals(1, mAppsGroup.getChildCount());
    }

    @Test
    public void testUpdateView_showSelectedUserItems() {
        mActivity.setConfigStore(mTestConfigStore);
        mState.action = State.ACTION_GET_CONTENT;
        mState.stack.changeRoot(TestProvidersAccess.RECENTS);
        final List<Item> rootList = new ArrayList<>();
        rootList.add(new RootItem(TestProvidersAccess.INSPECTOR, mActionHandler, mMaybeShowBadge));
        rootList.add(new RootItem(TestProvidersAccess.AUDIO, mActionHandler, mMaybeShowBadge));
        rootList.add(new RootItem(TestProvidersAccess.HAMMY, mActionHandler, mMaybeShowBadge));
        rootList.add(new RootItem(TestProvidersAccess.IMAGE, mActionHandler, mMaybeShowBadge));
        rootList.add(new RootItem(TestProvidersAccess.OtherUser.DOWNLOADS, mActionHandler,
                mMaybeShowBadge));
        rootList.add(new RootItem(TestProvidersAccess.OtherUser.PICKLES, mActionHandler,
                mMaybeShowBadge));
        mAppsRowManager.updateList(rootList);

        mAppsRowManager.updateView(mActivity);
        assertEquals(View.VISIBLE, mAppsRow.getVisibility());
        assertEquals(4, mAppsGroup.getChildCount());
    }

    @Test
    public void testUpdateView_showSelectedUserItems_otherUser() {
        if (!VersionUtils.isAtLeastR()) {
            return;
        }
        mActivity.setConfigStore(mTestConfigStore);
        mState.action = State.ACTION_GET_CONTENT;
        when(mActivity.getSelectedUser()).thenReturn(TestProvidersAccess.OtherUser.USER_ID);
        mState.stack.changeRoot(TestProvidersAccess.RECENTS);
        final List<Item> rootList = new ArrayList<>();
        rootList.add(new RootItem(TestProvidersAccess.INSPECTOR, mActionHandler, mMaybeShowBadge));
        rootList.add(new RootItem(TestProvidersAccess.AUDIO, mActionHandler, mMaybeShowBadge));
        rootList.add(new RootItem(TestProvidersAccess.HAMMY, mActionHandler, mMaybeShowBadge));
        rootList.add(new RootItem(TestProvidersAccess.IMAGE, mActionHandler, mMaybeShowBadge));
        rootList.add(new RootItem(TestProvidersAccess.OtherUser.DOWNLOADS, mActionHandler,
                mMaybeShowBadge));
        rootList.add(new RootItem(TestProvidersAccess.OtherUser.PICKLES, mActionHandler,
                mMaybeShowBadge));
        mAppsRowManager.updateList(rootList);

        mAppsRowManager.updateView(mActivity);
        assertEquals(View.VISIBLE, mAppsRow.getVisibility());
        assertEquals(2, mAppsGroup.getChildCount());
    }

    @Test
    public void testUpdateView_notInRecent_hideRow() {
        mActivity.setConfigStore(mTestConfigStore);
        mState.action = State.ACTION_BROWSE;
        final List<Item> rootList = new ArrayList<>();
        rootList.add(new RootItem(TestProvidersAccess.INSPECTOR, mActionHandler, mMaybeShowBadge));
        mAppsRowManager.updateList(rootList);

        mState.stack.changeRoot(TestProvidersAccess.DOWNLOADS);

        mAppsRowManager.updateView(mActivity);

        assertEquals(View.GONE, mAppsRow.getVisibility());
    }

    @Test
    public void testUpdateView_notHandledAction_hideRow() {
        mActivity.setConfigStore(mTestConfigStore);
        mState.action = State.ACTION_OPEN_TREE;

        mState.stack.changeRoot(TestProvidersAccess.RECENTS);
        final List<Item> rootList = new ArrayList<>();
        rootList.add(new RootItem(TestProvidersAccess.INSPECTOR, mActionHandler, mMaybeShowBadge));
        mAppsRowManager.updateList(rootList);

        mAppsRowManager.updateView(mActivity);

        assertEquals(View.GONE, mAppsRow.getVisibility());
    }

    @Test
    public void testUpdateView_noItems_hideRow() {
        mActivity.setConfigStore(mTestConfigStore);
        mState.action = State.ACTION_BROWSE;
        mState.stack.changeRoot(TestProvidersAccess.RECENTS);

        final List<Item> rootList = new ArrayList<>();
        mAppsRowManager.updateList(rootList);

        mAppsRowManager.updateView(mActivity);

        assertEquals(View.GONE, mAppsRow.getVisibility());
    }

    @Test
    public void testUpdateView_crossProfileSearch_hideRow() {
        mActivity.setConfigStore(mTestConfigStore);
        mState.supportsCrossProfile = true;
        when(mActivity.isSearchExpanded()).thenReturn(true);

        mState.stack.changeRoot(TestProvidersAccess.RECENTS);
        final List<Item> rootList = new ArrayList<>();
        rootList.add(new RootItem(TestProvidersAccess.INSPECTOR, mActionHandler, mMaybeShowBadge));
        rootList.add(new RootItem(TestProvidersAccess.AUDIO, mActionHandler, mMaybeShowBadge));
        rootList.add(new RootItem(TestProvidersAccess.HAMMY, mActionHandler, mMaybeShowBadge));
        rootList.add(new RootItem(TestProvidersAccess.IMAGE, mActionHandler, mMaybeShowBadge));
        rootList.add(new RootItem(TestProvidersAccess.OtherUser.DOWNLOADS, mActionHandler,
                mMaybeShowBadge));
        rootList.add(new RootItem(TestProvidersAccess.OtherUser.PICKLES, mActionHandler,
                mMaybeShowBadge));
        mAppsRowManager.updateList(rootList);
        mAppsRowManager.updateView(mActivity);

        assertThat(mAppsRow.getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void testUpdateView_notCrossProfileSearch_showRow() {
        mActivity.setConfigStore(mTestConfigStore);
        mState.supportsCrossProfile = true;
        when(mActivity.isSearchExpanded()).thenReturn(false);

        mState.stack.changeRoot(TestProvidersAccess.RECENTS);
        final List<Item> rootList = new ArrayList<>();
        rootList.add(new RootItem(TestProvidersAccess.INSPECTOR, mActionHandler, mMaybeShowBadge));
        rootList.add(new RootItem(TestProvidersAccess.AUDIO, mActionHandler, mMaybeShowBadge));
        rootList.add(new RootItem(TestProvidersAccess.HAMMY, mActionHandler, mMaybeShowBadge));
        rootList.add(new RootItem(TestProvidersAccess.IMAGE, mActionHandler, mMaybeShowBadge));
        rootList.add(new RootItem(TestProvidersAccess.OtherUser.DOWNLOADS, mActionHandler,
                mMaybeShowBadge));
        rootList.add(new RootItem(TestProvidersAccess.OtherUser.PICKLES, mActionHandler,
                mMaybeShowBadge));
        mAppsRowManager.updateList(rootList);
        mAppsRowManager.updateView(mActivity);

        assertThat(mAppsRow.getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test
    public void testUpdateView_noItemsOnSelectedUser_hideRow() {
        mActivity.setConfigStore(mTestConfigStore);
        mState.supportsCrossProfile = true;
        mState.stack.changeRoot(TestProvidersAccess.RECENTS);
        when(mActivity.getSelectedUser()).thenReturn(TestProvidersAccess.OtherUser.USER_ID);

        final List<Item> rootList = new ArrayList<>();
        rootList.add(new RootItem(TestProvidersAccess.INSPECTOR, mActionHandler, mMaybeShowBadge));
        rootList.add(new RootItem(TestProvidersAccess.AUDIO, mActionHandler, mMaybeShowBadge));
        rootList.add(new RootItem(TestProvidersAccess.HAMMY, mActionHandler, mMaybeShowBadge));
        rootList.add(new RootItem(TestProvidersAccess.IMAGE, mActionHandler, mMaybeShowBadge));
        mAppsRowManager.updateList(rootList);

        mAppsRowManager.updateView(mActivity);

        assertEquals(View.GONE, mAppsRow.getVisibility());
    }
}

